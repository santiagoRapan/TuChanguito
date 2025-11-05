import { testDataSource } from './setup';
import * as ListItemService from '../services/listItem.service';
import { User } from '../entities/user';
import { List } from '../entities/list';
import { Product } from '../entities/product';
import { ListItem } from '../entities/listItem';

describe('ListItem Service - Unit tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository(ListItem).clear();
    await testDataSource.getRepository(List).clear();
    await testDataSource.getRepository(Product).clear();
    await testDataSource.getRepository(User).clear();
  });

  it('should add, get, update, toggle and delete a list item', async () => {
    const userRepo = testDataSource.getRepository(User);
    const listRepo = testDataSource.getRepository(List);
    const productRepo = testDataSource.getRepository(Product);

    const user: any = await userRepo.save(userRepo.create({ name: 'LIUser', surname: 'Owner', email: 'liuser@test.com', password: 'x', isVerified: true } as any));
    const list: any = await listRepo.save(listRepo.create({ name: 'LList', description: 'desc', owner: user } as any));
    const product: any = await productRepo.save(productRepo.create({ name: 'Bread', owner: user } as any));

    const added: any = await ListItemService.addListItemService({ listId: list.id, product: { id: product.id }, quantity: 3, unit: 'pcs', owner: user } as any);
    expect(added).toBeDefined();
    const item = added.item || added;
    expect(item.quantity).toBeDefined();

    const result: any = await ListItemService.getListItemsService({ listId: list.id, page: 1, per_page: 10 } as any);
    expect(Array.isArray(result.data)).toBe(true);
    expect(result.pagination).toBeDefined();
    expect(result.data.length).toBeGreaterThanOrEqual(1);

    const updated: any = await ListItemService.updateListItemService(list.id, item.id, { quantity: 5 } as any);
    expect(updated.quantity).toBeDefined();

    const toggled: any = await ListItemService.toggleListItemPurchasedService(list.id, item.id, true as any);
    expect(toggled.purchased).toBe(true);

    const deleted = await ListItemService.deleteListItemService(list.id, item.id);
    expect(deleted).toBe(true);
  });
});
