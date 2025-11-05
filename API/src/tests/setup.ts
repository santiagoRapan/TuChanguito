// TODO: Update setup
import { DataSource } from 'typeorm';
import dotenv from 'dotenv';
import { createTransport } from 'nodemailer';
import { Mailer } from '../services/email.service';
import { app } from '../app';

// Import all entities used in the project so the test DataSource knows them
import { User } from '../entities/user';
import { UserVerificationToken } from '../entities/userVerificationToken';
import { UserPasswordRecoveryToken } from '../entities/userPasswordRecoveryToken';
import { Category } from '../entities/category';
import { List } from '../entities/list';
import { ListItem } from '../entities/listItem';
import { Pantry } from '../entities/pantry';
import { PantryItem } from '../entities/pantryItem';
import { Product } from '../entities/product';
import { Purchase } from '../entities/purchase';

dotenv.config();

// Set JWT token for tests
process.env.JWT_TOKEN = process.env.JWT_TOKEN || 'test-secret-key';

const testDataSource = new DataSource({
  type: 'sqlite',
  database: ':memory:',
  dropSchema: true,
  entities: [
    User,
    UserVerificationToken,
    UserPasswordRecoveryToken,
    Category,
    List,
    ListItem,
    Pantry,
    PantryItem,
    Product,
    Purchase
  ],
  synchronize: true,
  logging: false
});

beforeAll(async () => {
  await testDataSource.initialize();

  try {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const dbModule = require('../db');
    if (dbModule) dbModule.default = testDataSource;
  } catch (err) {
    console.warn('Could not monkey-patch db module for tests', err);
  }

  // Setup mock mailer
  const mockTransporter = createTransport({
    host: 'smtp.ethereal.email',
    port: 587,
    auth: {
      user: 'test@ethereal.email',
      pass: 'testpass'
    }
  });
  const mailer = new Mailer(mockTransporter);
  if (typeof (global as any).jest !== 'undefined') {
    // @ts-ignore
    mailer.sendEmail = (global as any).jest.fn();
  } else {
    // @ts-ignore
    mailer.sendEmail = async () => {};
  }
  app.locals.mailer = mailer;

  if (typeof (global as any).jest !== 'undefined') {
    // @ts-ignore
    (global as any).jest.setTimeout(30000);
  }
});

afterAll(async () => {
  if (testDataSource && testDataSource.isInitialized) {
    await testDataSource.destroy();
  }
});

export { testDataSource };
