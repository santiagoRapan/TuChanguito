import { testDataSource } from './setup';
import * as CategoryService from '../services/category.service';
import { Category } from '../entities/category';
import { User } from '../entities/user';

describe('Category Service - Unit tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository('category').clear();
    await testDataSource.getRepository('user').clear();
  });

  it('should create a new category', async () => {
    const userRepo = testDataSource.getRepository(User);
    const user = userRepo.create({ name: 'Cat', surname: 'Owner', email: 'catowner@test.com', password: 'x', isVerified: true });
    await userRepo.save(user);

    const result = await CategoryService.createNewCategoryService({ name: 'Fruits', owner: user });
    expect(result).toBeDefined();
    expect((result as any).name).toBe('Fruits');
  });

  it('should list user categories', async () => {
    const userRepo = testDataSource.getRepository(User);
    const user = await userRepo.save(userRepo.create({ name: 'List', surname: 'Owner', email: 'listowner@test.com', password: 'x', isVerified: true }));

    await CategoryService.createNewCategoryService({ name: 'A', owner: user });
    await CategoryService.createNewCategoryService({ name: 'B', owner: user });

    const result = await CategoryService.getUserCategoriesService({ owner: user, page: '1', per_page: 10, sort_by: 'createdAt', order: 'DESC' });
    expect(Array.isArray(result.data)).toBe(true);
    expect(result.data.length).toBeGreaterThanOrEqual(2);
    expect(result.pagination).toBeDefined();
  });

  it('should get category by id', async () => {
    const userRepo = testDataSource.getRepository(User);
    const user = await userRepo.save(userRepo.create({ name: 'Get', surname: 'Owner', email: 'getowner@test.com', password: 'x', isVerified: true }));

    const created = await CategoryService.createNewCategoryService({ name: 'Vegetables', owner: user });
    const catObj: any = created;

    const fetched = await CategoryService.getCategoryByIdService(catObj.id);
    expect(fetched).toBeDefined();
    expect((fetched as any).name).toBe('Vegetables');
  });

  it('should update category', async () => {
    const userRepo = testDataSource.getRepository(User);
    const user = await userRepo.save(userRepo.create({ name: 'Up', surname: 'Owner', email: 'upowner@test.com', password: 'x', isVerified: true }));

    const created = await CategoryService.createNewCategoryService({ name: 'OldName', owner: user });
    const catObj: any = created;

    const updated = await CategoryService.updateCategoryService(catObj.id, 'NewName', { foo: 'bar' });
    expect((updated as any).name).toBe('NewName');
    expect((updated as any).metadata).toBeDefined();
  });

  it('should delete category', async () => {
    const userRepo = testDataSource.getRepository(User);
    const user = await userRepo.save(userRepo.create({ name: 'Del', surname: 'Owner', email: 'delowner@test.com', password: 'x', isVerified: true }));

    const created = await CategoryService.createNewCategoryService({ name: 'ToDelete', owner: user });
    const catObj: any = created;

    const deleted = await CategoryService.deleteCategoryService(user, catObj.id);
    expect(deleted).toBe(true);

    const found = await testDataSource.getRepository(Category).findOne({ where: { id: catObj.id, deletedAt: null } as any });
    expect(found).toBeNull();
  });
});

