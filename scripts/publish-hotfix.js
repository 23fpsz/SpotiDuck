const { GoogleAuth } = require('google-auth-library');
const fs = require('fs');
const path = require('path');

async function main() {
  const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!serviceAccountPath || !fs.existsSync(serviceAccountPath)) {
    console.error('Error: GOOGLE_APPLICATION_CREDENTIALS environment variable not set or file does not exist.');
    process.exit(1);
  }

  const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
  const projectId = serviceAccount.project_id;
  if (!projectId) {
    console.error('Error: Project ID not found in service account file.');
    process.exit(1);
  }

  console.log(`Authenticating with project: ${projectId}`);
  const auth = new GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/firebase.remoteconfig']
  });
  const client = await auth.getClient();

  // 1. Get current Remote Config template using client.request()
  console.log('Fetching current Remote Config template from Firebase...');
  const getUrl = `https://firebaseremoteconfig.googleapis.com/v1/projects/${projectId}/remoteConfig`;
  
  const getResponse = await client.request({
    url: getUrl,
    method: 'GET'
  });

  const template = getResponse.data;
  const etag = typeof getResponse.headers.get === 'function' 
    ? getResponse.headers.get('etag') 
    : (getResponse.headers['etag'] || getResponse.headers['ETag']);
  console.log(`Current ETag: ${etag}`);

  // 2. Read local asset files and update template parameters
  const assetsDir = path.join(__dirname, '../app/src/main/assets');
  const hotfixFiles = [
    { file: 'spotify_bridge.js', key: 'hotfix_spotify_bridge' },
    { file: 'css_hacks.css', key: 'hotfix_css_hacks' },
    { file: 'classic_login.js', key: 'hotfix_classic_login' },
    { file: 'desktop_spoof.js', key: 'hotfix_desktop_spoof' },
    { file: 'facebook_consent.js', key: 'hotfix_facebook_consent' }
  ];

  let updated = false;
  if (!template.parameters) {
    template.parameters = {};
  }

  for (const item of hotfixFiles) {
    const filePath = path.join(assetsDir, item.file);
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, 'utf8').trim();
      
      const currentParam = template.parameters[item.key] || {};
      const currentVal = currentParam.defaultValue ? currentParam.defaultValue.value : '';

      if (currentVal !== content) {
        console.log(`Updating ${item.key} with local changes of ${item.file}...`);
        template.parameters[item.key] = {
          defaultValue: {
            value: content
          },
          valueType: 'STRING'
        };
        updated = true;
      } else {
        console.log(`${item.key} is already up to date.`);
      }
    }
  }

  if (!updated) {
    console.log('Remote config is already fully up to date. No publish needed.');
    return;
  }

  // 3. Put updated Remote Config template using client.request()
  console.log('Publishing updated template to Firebase...');
  const putResponse = await client.request({
    url: getUrl,
    method: 'PUT',
    headers: {
      'If-Match': etag
    },
    data: template
  });

  if (putResponse.status === 200) {
    console.log('Firebase Remote Config successfully updated and published!');
  } else {
    console.error(`Failed to publish template. Status: ${putResponse.status}`);
    console.error(putResponse.data);
    process.exit(1);
  }
}

main().catch((err) => {
  console.error('Fatal error running hotfix publisher script:', err);
  process.exit(1);
});
