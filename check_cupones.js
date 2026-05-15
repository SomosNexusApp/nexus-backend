
const fetch = require('node-fetch');

async function checkCupones() {
  const url = 'http://localhost:8080/api/admin/cupones?page=0&size=20';
  const auth = 'Basic ' + Buffer.from('admin:admin').toString('base64'); // Assuming default admin:admin
  
  try {
    const res = await fetch(url, {
      headers: { 'Authorization': auth }
    });
    const data = await res.json();
    console.log('Cupones found:', data.totalElements);
    console.log('Content:', JSON.stringify(data.content, null, 2));
  } catch (e) {
    console.error('Error fetching cupones:', e.message);
  }
}

checkCupones();
