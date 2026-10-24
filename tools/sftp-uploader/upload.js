const fs = require('fs');
const path = require('path');
const readline = require('readline');
const Client = require('ssh2-sftp-client');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

const askQuestion = (query) => new Promise((resolve) => rl.question(query, resolve));

async function main() {
  console.log('========================================================');
  console.log('       BOTKEEP SFTP AUTO-UPLOAD TOOL (ERP BACKEND)');
  console.log('========================================================\n');

  const jarPath = path.resolve(__dirname, '../../erp-backend/target/erp-backend-0.0.1-SNAPSHOT.jar');

  if (!fs.existsSync(jarPath)) {
    console.error(`❌ KHONG TIM THAY FILE JAR TAI:\n${jarPath}\n`);
    console.log('👉 Vui long chay file "build-backend.bat" truoc de build file .jar nhe!\n');
    rl.close();
    process.exit(1);
  }

  const stats = fs.statSync(jarPath);
  const fileSizeMb = (stats.size / (1024 * 1024)).toFixed(2);
  console.log(`📦 Tim thay file JAR: erp-backend-0.0.1-SNAPSHOT.jar (${fileSizeMb} MB)\n`);

  console.log('--- NHAP THONG TIN TU TAB "SFTP" TREN BOTKEEP ---');
  let host = await askQuestion('1. Host (Enter de mac dinh "node5.botkeep.cloud"): ');
  host = host.trim() || 'node5.botkeep.cloud';

  let port = await askQuestion('2. Port (Enter de mac dinh 2026): ');
  port = parseInt(port.trim(), 10) || 2026;

  let username = await askQuestion('3. Username (bksftp-...): ');
  username = username.trim();
  if (!username) {
    console.error('❌ Username khong duoc de trong!');
    rl.close();
    process.exit(1);
  }

  let password = await askQuestion('4. Password: ');
  password = password.trim();
  if (!password) {
    console.error('❌ Password khong duoc de trong!');
    rl.close();
    process.exit(1);
  }

  console.log('\n⏳ Dang ket noi toi Botkeep SFTP...');
  const sftp = new Client();

  try {
    await sftp.connect({ host, port, username, password });
    console.log('✅ Ket noi thanh cong!\n');

    console.log(`🚀 Dang tai ${fileSizeMb} MB len Botkeep server...`);
    const remotePath = './erp-backend-0.0.1-SNAPSHOT.jar';

    await sftp.fastPut(jarPath, remotePath, {
      step: (transferred, chunk, total) => {
        const percent = ((transferred / total) * 100).toFixed(1);
        const transferredMb = (transferred / (1024 * 1024)).toFixed(1);
        process.stdout.write(`   [TIEN DO]: ${percent}% (${transferredMb} MB / ${fileSizeMb} MB)\r`);
      }
    });

    console.log('\n\n========================================================');
    console.log('🎉 [HOAN TAT] File .jar da duoc upload thanh cong 100%!');
    console.log('👉 Buoc tiep theo: Quay lai Botkeep va bam nut "RESTART"!');
    console.log('========================================================\n');
  } catch (err) {
    console.error('\n❌ Loi SFTP:', err.message || err);
    console.log('👉 Vui long kiem tra lai Username / Password xem co bi het han 60 phut khong nhe.\n');
  } finally {
    await sftp.end();
    rl.close();
  }
}

main();

