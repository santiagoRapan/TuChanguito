import { testDataSource } from './setup';
import * as PantryService from '../services/pantry.service';
import { User } from '../entities/user';
import { Pantry } from '../entities/pantry';
import { Product } from '../entities/product';
import { Category } from '../entities/category';

describe('Pantry Service - Unit tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository(Product).clear();
    await testDataSource.getRepository(Category).clear();
    await testDataSource.getRepository(Pantry).clear();
    await testDataSource.getRepository(User).clear();
  });

  it('should create, fetch, update and delete a pantry', async () => {
    const userRepo = testDataSource.getRepository(User);
    const user: any = await userRepo.save(userRepo.create({ name: 'PUser', surname: 'Owner', email: 'puser@test.com', password: 'x', isVerified: true } as any));

    const created: any = await PantryService.createPantryService({ name: 'Home Pantry' } as any, user as any);
    expect(created).toBeDefined();
    expect(created.name).toBe('Home Pantry');

    const result: any = await PantryService.getPantriesService(user as any, true, 'createdAt', 'ASC', 1, 10);
    expect(Array.isArray(result.data)).toBe(true);
    expect(result.pagination).toBeDefined();
    expect(result.data.length).toBeGreaterThanOrEqual(1);

    const fetched: any = await PantryService.getPantryByIdService(created.id, user as any);
    expect(fetched).toBeDefined();
    expect(fetched.name).toBe('Home Pantry');

    const updated: any = await PantryService.updatePantryService(created.id, 'Updated Pantry', undefined, user as any);
    expect(updated.name).toBe('Updated Pantry');

    await PantryService.deletePantryService(created.id, user as any);
    await expect(PantryService.getPantryByIdService(created.id, user as any)).rejects.toBeDefined();
  });

  it('should share a pantry with another user and revoke access', async () => {
    const userRepo = testDataSource.getRepository(User);
    const userA: any = await userRepo.save(userRepo.create({ name: 'Owner', surname: 'A', email: 'ownerA@test.com', password: 'x', isVerified: true } as any));
    const userB: any = await userRepo.save(userRepo.create({ name: 'Shared', surname: 'B', email: 'sharedB@test.com', password: 'x', isVerified: true } as any));

    const pantry: any = await PantryService.createPantryService({ name: 'SharedPantry' } as any, userA as any);

    const sharedWith = await PantryService.sharePantryService(pantry.id, userA as any, 'sharedB@test.com');
    expect(sharedWith).toBeDefined();
    expect(sharedWith.id).toBe(userB.id);

    const users = await PantryService.getSharedUsersService(pantry.id, userA as any);
    expect(Array.isArray(users)).toBe(true);
    expect(users.some((u: any) => u.id === userB.id)).toBe(true);

    await PantryService.revokePantryShareService(pantry.id, userA as any, userB.id);

    const usersAfter = await PantryService.getSharedUsersService(pantry.id, userA as any);
    expect(usersAfter.some((u: any) => u.id === userB.id)).toBe(false);
  });
});
