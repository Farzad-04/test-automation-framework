import { rm } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const staticDirectory = fileURLToPath(new URL('../../src/main/resources/static', import.meta.url));
await rm(staticDirectory, { recursive: true, force: true });
