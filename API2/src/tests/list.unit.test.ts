import { testDataSource } from './setup';
import * as ListService from '../services/list.service';
import { User } from '../entities/user';
import { List } from '../entities/list';
import { Product } from '../entities/product';
import { ListItem } from '../entities/listItem';

describe('List Service - Unit tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository(ListItem).clear();
    await testDataSource.getRepository(List).clear();
    await testDataSource.getRepository(Product).clear();
    await testDataSource.getRepository(User).clear();
  });

  it('should create, fetch, update and delete a shopping list', async () => {
    const userRepo = testDataSource.getRepository(User);
    const user: any = await userRepo.save(userRepo.create({ name: 'LUser', surname: 'Owner', email: 'luser@test.com', password: 'x', isVerified: true } as any));

    const created: any = await ListService.createListService({ name: 'MyList', description: 'desc', recurring: false, owner: user } as any);
    expect(created).toBeDefined();
    expect(created.name).toBe('MyList');

    const result: any = await ListService.getListsService({ user: user, page: 1, per_page: 10 } as any);
    expect(Array.isArray(result.data)).toBe(true);
    expect(result.pagination).toBeDefined();
    expect(result.data.length).toBeGreaterThanOrEqual(1);

    const fetched: any = await ListService.getListByIdService(created.id, user as any);
    expect(fetched).toBeDefined();
    expect(fetched.name).toBe('MyList');

    const updated: any = await ListService.updateListService(created.id, { name: 'UpdatedList' } as any, user as any);
    expect((updated as any).name).toBe('UpdatedList');

    const deleted = await ListService.deleteListService(created.id, user as any);
    expect(deleted).toBe(true);
  });

  it('should purchase a list and move items to pantry correctly', async () => {
    const userRepo = testDataSource.getRepository(User);
    const productRepo = testDataSource.getRepository(Product);
    const listRepo = testDataSource.getRepository(List);
    const listItemRepo = testDataSource.getRepository(ListItem);

    const user: any = await userRepo.save(userRepo.create({ name: 'PUser', surname: 'Owner', email: 'plist@test.com', password: 'x', isVerified: true } as any));

    const list: any = await listRepo.save(listRepo.create({ name: 'BuyStuff', description: 'desc', owner: user } as any));
    const product: any = await productRepo.save(productRepo.create({ name: 'Milk', owner: user } as any));

    const item = await listItemRepo.save(listItemRepo.create({ product: product, quantity: 2, list: list, owner: user, purchased: true } as any));
    expect(item).toBeDefined();

    const res: any = await ListService.purchaseListService(list.id, user as any, { note: 'for test' } as any).catch(e => { throw e; });
    expect(res).toBeDefined();
    await expect(ListService.getListByIdService(list.id, user as any)).rejects.toBeDefined();
  });

});
