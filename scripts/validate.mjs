import { spawnSync } from 'node:child_process';

const commands = [
  ['./node_modules/eslint/bin/eslint.js', 'src', 'app.config.ts', '--max-warnings=0'],
  ['./node_modules/typescript/bin/tsc', '--noEmit'],
  ['./node_modules/vitest/vitest.mjs', 'run'],
];

for (const args of commands) {
  const result = spawnSync(process.execPath, args, { stdio: 'inherit' });
  if (result.status !== 0) process.exit(result.status ?? 1);
}
