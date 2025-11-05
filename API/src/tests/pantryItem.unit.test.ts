import { testDataSource } from './setup';
import * as PantryItemService from '../services/pantryItem.service';
import { User } from '../entities/user';
import { Pantry } from '../entities/pantry';
import { Product } from '../entities/product';
import { Category } from '../entities/category';
import { PantryItem } from '../entities/pantryItem';

describe('PantryItem Service - Unit tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository(PantryItem).clear();
    await testDataSource.getRepository(Product).clear();
    await testDataSource.getRepository(Category).clear();
    await testDataSource.getRepository(Pantry).clear();
    await testDataSource.getRepository(User).clear();
  });

  it('should add, get, update and delete a pantry item', async () => {
    const userRepo = testDataSource.getRepository(User);
    const pantryRepo = testDataSource.getRepository(Pantry);
    const categoryRepo = testDataSource.getRepository(Category);
    const productRepo = testDataSource.getRepository(Product);

    const user: any = await userRepo.save(userRepo.create({ name: 'PIUser', surname: 'Owner', email: 'piuser@test.com', password: 'x', isVerified: true } as any));
    const pantry: any = await pantryRepo.save(pantryRepo.create({ name: 'Pantry1', owner: user } as any));
    const category: any = await categoryRepo.save(categoryRepo.create({ name: 'Cat1', owner: user } as any));
    const product: any = await productRepo.save(productRepo.create({ name: 'Cheese', owner: user, pantry: pantry, category: category } as any));

    const added: any = await PantryItemService.addPantryItemService(pantry.id, user as any, { product: { id: product.id }, quantity: 2, unit: 'pcs' } as any);
    expect(added).toBeDefined();
    const item = added;
    expect(item.quantity).toBeDefined();

    const result: any = await PantryItemService.getPantryItemsService(pantry.id, user as any, 1, 10, 'DESC');
    expect(Array.isArray(result.data)).toBe(true);
    expect(result.pagination).toBeDefined();
    expect(result.data.length).toBeGreaterThanOrEqual(1);

    const updated: any = await PantryItemService.updatePantryItemService(pantry.id, item.id, user as any, { quantity: 5 } as any);
    expect((updated as any).quantity).toBeDefined();

    await PantryItemService.deletePantryItemService(pantry.id, item.id, user as any);
    const resultAfter: any = await PantryItemService.getPantryItemsService(pantry.id, user as any, 1, 10, 'DESC');
    expect(Array.isArray(resultAfter.data)).toBe(true);
    expect(resultAfter.pagination).toBeDefined();
  });
});
