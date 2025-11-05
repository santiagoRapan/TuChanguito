import { test, expect } from "@jest/globals";
import { FindOptionsOrderValue } from "typeorm";
import { checkDirectionParam } from "../../utils/params";

test('Test checkDirectionParam when given a valid param value', () => {
  const response: FindOptionsOrderValue = checkDirectionParam('ASC');
  expect(response).toEqual('ASC');
});

test('Test checkDirectionParam when given an invalid param value', () => {
  const response: FindOptionsOrderValue = checkDirectionParam('WRONG');
  expect(response).toEqual('DESC');
});
