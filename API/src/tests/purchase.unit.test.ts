import { testDataSource } from './setup';
import * as PurchaseService from '../services/purchase.service';
import * as ListService from '../services/list.service';
import { User } from '../entities/user';
import { List } from '../entities/list';
import { Product } from '../entities/product';
import { ListItem } from '../entities/listItem';
import { Purchase } from '../entities/purchase';

describe('Purchase Service - Unit tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository(ListItem).clear();
    await testDataSource.getRepository(List).clear();
    await testDataSource.getRepository(Product).clear();
    await testDataSource.getRepository(User).clear();
    await testDataSource.getRepository(Purchase).clear();
  });

  it('should create a purchase from a list and retrieve it, then restore', async () => {
    const userRepo = testDataSource.getRepository(User);
    const productRepo = testDataSource.getRepository(Product);
    const listRepo = testDataSource.getRepository(List);
    const listItemRepo = testDataSource.getRepository(ListItem);
    const purchaseRepo = testDataSource.getRepository(Purchase);

    const user: any = await userRepo.save(userRepo.create({ name: 'PU', surname: 'Owner', email: 'purchase@test.com', password: 'x', isVerified: true } as any));
    const list: any = await listRepo.save(listRepo.create({ name: 'BuyX Unit Test', description: 'desc', owner: user } as any));
    const product: any = await productRepo.save(productRepo.create({ name: 'Eggs', owner: user } as any));

    const item = await listItemRepo.save(listItemRepo.create({ product: product, quantity: 6, list: list, owner: user, purchased: true } as any));
    expect(item).toBeDefined();

    const purchasedList = await ListService.purchaseListService(list.id, user as any, { note: 'unit' } as any);
    expect(purchasedList).toBeDefined();

    const purchases = await purchaseRepo.find({ relations: ['items', 'owner', 'list'], withDeleted: true });
    expect(purchases.length).toBeGreaterThanOrEqual(1);
    const purchase = purchases[0];

    const result = await PurchaseService.getPurchasesService({ user: user, page: 1, per_page: 10 } as any);
    expect(Array.isArray(result.data)).toBe(true);
    expect(result.pagination).toBeDefined();

    const fetched = await PurchaseService.getPurchaseByIdService(purchase.id, user as any);
    expect(fetched).toBeDefined();
    expect(fetched.id).toBe(purchase.id);

    const restored = await PurchaseService.restorePurchaseService(purchase.id, user as any);
    expect(restored).toBeDefined();
    expect(restored.name).toBeDefined();
  });
});
