import { expect, test } from '@jest/globals';
import { 
  isValidRegistrationData,
  isValidLoginData,
  isValidVerificationTokenData,
  isValidPasswordRecoveryData,
  isValidPasswordResetData
} from '../../types/user';


test('Test isValidRegistrationData, when receiving valid data, it should return true', () => {
  const validRegistrationData = {
    email: 'john_doe@mail.com',
    password: '123456',
    name: 'John',
    surname: 'Doe',
  };
  const result = isValidRegistrationData(validRegistrationData);
  expect(result.isValid).toBe(true);
});

test('Test isValidRegistrationData, when receiving invalid data, it should return true', () => {
  const invalidRegistrationData = {
    password: '123456',
    name: 'John',
    surname: 'Doe',
  };
  const result = isValidRegistrationData(invalidRegistrationData);
  expect(result.isValid).toBe(false);
});

test('Test isValidLoginData, when receiving valid data, it should return true', () => {
  const validLoginData = {
    email: 'john_doe@mail.com',
    password: '123456'
  };
  const result = isValidLoginData(validLoginData);
  expect(result.isValid).toBe(true);
});

test('Test isValidLoginData, when receiving invalid data, it should return false', () => {
  const invalidLoginData = {
    email: 'john_doe@mail.com'
  };

  const result = isValidLoginData(invalidLoginData);
  expect(result.isValid).toBe(false);
});

test('Test isValidVerificationTokenData, when receiving valid data, it should return true', () => {
  const validVerificationTokenData = {
    code: 'abc123'
  };
  const result = isValidVerificationTokenData(validVerificationTokenData);
  expect(result.isValid).toBe(true);

});

test('Test isValidVerificationTokenData, when receiving invalid data, it should return true', () => {
  const invalidVerificationTokenData = {};
  const result = isValidVerificationTokenData(invalidVerificationTokenData);
  expect(result.isValid).toBe(false);
});

test('Test isValidPasswordRecoveryData, when receiving valid data, it should return true', () => {
  const validPasswordRecoveryData = {
    email: 'john_doe@mail.com'
  };
  const result = isValidPasswordRecoveryData(validPasswordRecoveryData);
  expect(result.isValid).toBe(true);
});

test('Test isValidPasswordRecoveryData, when receiving invalid data, it should return true', () => {
  const invalidPasswordRecoveryData = {};
  const result = isValidPasswordRecoveryData(invalidPasswordRecoveryData);
  expect(result.isValid).toBe(false);
});

test('Test isValidPasswordResetData, when receiving valid data, it should return true', () => {
  const validPasswordResetData = {
    code: 'abc123',
    password: '1234567'
  };
  const result = isValidPasswordResetData(validPasswordResetData);
  expect(result.isValid).toBe(true);
});

test('Test isValidPasswordResetData, when receiving invalid data, it should return true', () => {
  const invalidPasswordResetData = {};
  const result = isValidPasswordResetData(invalidPasswordResetData);
  expect(result.isValid).toBe(false);
});