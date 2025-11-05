import { testDataSource } from './setup';
import * as ProductService from '../services/product.service';
import { User } from '../entities/user';
import { Category } from '../entities/category';
import { Pantry } from '../entities/pantry';

describe('Product Service - Unit tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository(ProductService as any).manager.getRepository('product').clear();
    await testDataSource.getRepository(Category).clear();
    await testDataSource.getRepository(Pantry).clear();
    await testDataSource.getRepository(User).clear();
  });

  it('should create, fetch, update and delete a product', async () => {
    const userRepo = testDataSource.getRepository(User);
    const categoryRepo = testDataSource.getRepository(Category);
    const pantryRepo = testDataSource.getRepository(Pantry);

    const user: any = await userRepo.save(userRepo.create({ name: 'PUser', surname: 'Owner', email: 'puser@test.com', password: 'x', isVerified: true } as any));
    const category: any = await categoryRepo.save(categoryRepo.create({ name: 'Dairy', owner: user } as any));
    const pantry: any = await pantryRepo.save(pantryRepo.create({ name: 'Home', owner: user } as any));

    const created: any = await ProductService.createProductService({ name: 'Milk', category: { id: category.id }, owner: user } as any);
    expect(created).toBeDefined();
    expect(created.name).toBe('Milk');

    const result: any = await ProductService.getProductsService({ owner: user, page: 1, per_page: 10, sort_by: 'name', order: 'ASC' } as any);
    expect(Array.isArray(result.data)).toBe(true);
    expect(result.pagination).toBeDefined();
    expect(result.data.length).toBeGreaterThanOrEqual(1);

    const prodObj: any = created;
    const fetched: any = await ProductService.getProductByIdService(prodObj.id, user as any);
    expect(fetched).toBeDefined();
    expect(fetched.name).toBe('Milk');

    const updated: any = await ProductService.updateProductService(prodObj.id, user as any, { name: 'Skim Milk' } as any);
    expect(updated.name).toBe('Skim Milk');

    const deleted: any = await ProductService.deleteProductService(prodObj.id, user as any);
    expect(deleted).toBe(true);
  });

  it('should filter products by pantry_id and category_id', async () => {
    const userRepo = testDataSource.getRepository(User);
    const categoryRepo = testDataSource.getRepository(Category);
    const pantryRepo = testDataSource.getRepository(Pantry);

    const user: any = await userRepo.save(userRepo.create({ name: 'Filter', surname: 'Owner', email: 'filter@test.com', password: 'x', isVerified: true } as any));
    const cat1: any = await categoryRepo.save(categoryRepo.create({ name: 'C1', owner: user } as any));
    const cat2: any = await categoryRepo.save(categoryRepo.create({ name: 'C2', owner: user } as any));
    const pantry1: any = await pantryRepo.save(pantryRepo.create({ name: 'Pan1', owner: user } as any));
    const pantry2: any = await pantryRepo.save(pantryRepo.create({ name: 'Pan2', owner: user } as any));

    await ProductService.createProductService({ name: 'P1', category: { id: cat1.id }, owner: user } as any);
    await ProductService.createProductService({ name: 'P2', category: { id: cat2.id }, owner: user } as any);

    const res1: any = await ProductService.getProductsService({ owner: user, page: 1, per_page: 10, category_id: cat1.id } as any);
    expect(res1.data.length).toBeGreaterThanOrEqual(1);

    const res2: any = await ProductService.getProductsService({ owner: user, page: 1, per_page: 10, pantry_id: pantry2.id } as any);
    expect(res2.data.length).toBeGreaterThanOrEqual(1);
  });
});
