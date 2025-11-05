import { generateUserToken } from "../../utils/tokens";
import { test, expect } from "@jest/globals";

test('Test token generation method', () => {
  const token: string = generateUserToken();
  expect(token).toHaveLength(16);
});