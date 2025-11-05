import "reflect-metadata";
import { test, expect, beforeAll, afterAll } from "@jest/globals";
import { User } from "../../entities/user";
import { removeUserPrivateValues } from "../../utils/users";
import { testDataSource } from "../../db.test"

beforeAll(async () => {
  await testDataSource.initialize();
});

afterAll(async () => {
  await testDataSource.destroy();
});

test('test private fields removal', async () => {
  const user: User = new User();
  user.name = 'Test';
  user.surname = 'Test';
  user.email = 'test@mail.com';
  user.password = 'abc123';
  user.metadata = { age: 30 };
  await user.save();
  removeUserPrivateValues(user);
  expect(user.isVerified).toBeUndefined();
  expect(user.password).toBeUndefined();
  expect([undefined, null].includes(user.updatedAt as any) || user.updatedAt instanceof Date).toBe(true);
});