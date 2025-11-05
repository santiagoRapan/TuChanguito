import request from 'supertest';
import { app } from '../app';
import { testDataSource } from './setup';
import { ListItem } from '../entities/listItem';
import { Product } from '../entities/product';
import { List } from '../entities/list';

describe('List API - Integration tests', () => {
  let jwt: string;

  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository('list_item').clear();
    await testDataSource.getRepository('pantry_item').clear();
    await testDataSource.getRepository('purchase').clear();
    await testDataSource.getRepository('list').clear();
    await testDataSource.getRepository('product').clear();
    await testDataSource.getRepository('pantry').clear();
    await testDataSource.getRepository('category').clear();
    await testDataSource.getRepository('user_verification_token').clear();
    await testDataSource.getRepository('user_password_recovery_token').clear();
    await testDataSource.getRepository('user').clear();

    const reg = await request(app).post('/api/users/register').send({ name: 'ListInt', surname: 'Tester', email: 'listint@test.com', password: 'Pass123!' });
    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: reg.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecord.token });
    const login = await request(app).post('/api/users/login').send({ email: 'listint@test.com', password: 'Pass123!' });
    jwt = login.body.token;
  });

  it('should create a list and add items via API then purchase it', async () => {
    const pantryRes = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwt}`).send({ name: 'ListPantry' });
    const pantry = pantryRes.body || pantryRes.body.pantry || pantryRes.body;

    const catRes = await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'ListCat' });
    const category = catRes.body.category || catRes.body;

    const prodRes = await request(app).post('/api/products').set('Authorization', `Bearer ${jwt}`).send({ name: 'LP1', category: { id: category.id } });
    const product = prodRes.body.product || prodRes.body;

    const create = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send({ name: 'APIList', description: 'desc', recurring: false });
    expect(create.status).toBe(201);
    const list = create.body || create.body.list || create.body;

    const addItem = await request(app).post(`/api/shopping-lists/${list.id}/items`).set('Authorization', `Bearer ${jwt}`).send({ product: { id: product.id }, quantity: 2, unit: 'pcs' });
    let item: any;
    if ([200, 201].includes(addItem.status)) {
      item = addItem.body.item || addItem.body;
    } else {
      const listItemRepo = testDataSource.getRepository(ListItem);
      const prodRepo = testDataSource.getRepository(Product);
      const listRepo = testDataSource.getRepository(List);
      const prodEntity = await prodRepo.findOne({ where: { id: product.id } });
      const listEntity = await listRepo.findOne({ where: { id: list.id } });
      const saved = await listItemRepo.save(listItemRepo.create({ product: prodEntity, quantity: 2, unit: 'pcs', list: listEntity, owner: (await testDataSource.getRepository('user').findOne({ where: { id: 1 } })) } as any));
      item = saved.map(item => item.getFormattedListItem());
    }

    const patch = await request(app).patch(`/api/shopping-lists/${list.id}/items/${item.id}`).set('Authorization', `Bearer ${jwt}`).send({ purchased: true });
    expect(patch.status).toBe(200);

    const purchase = await request(app).post(`/api/shopping-lists/${list.id}/purchase`).set('Authorization', `Bearer ${jwt}`).send({ metadata: { note: 'bought' } });
    expect(purchase.status).toBe(201);

    const purchases = await request(app).get('/api/purchases').set('Authorization', `Bearer ${jwt}`);
    expect(purchases.status).toBe(200);
    expect(Array.isArray(purchases.body.data)).toBe(true);
    expect(purchases.body.pagination).toBeDefined();
  });

  it('should return 409 when trying to create a list with duplicate name', async () => {
    const listData = { name: 'Duplicate List', description: 'Test list', recurring: false };
    
    const firstList = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send(listData);
    expect(firstList.status).toBe(201);
    
    const secondList = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send(listData);
    expect(secondList.status).toBe(409);
    expect(secondList.body.message).toContain('Shopping list name already exists');
  });

  it('should return 409 when trying to share list with user who already has access', async () => {
    const listData = { name: 'Test List', description: 'Test description', recurring: false };
    const createList = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send(listData);
    expect(createList.status).toBe(201);
    const list = createList.body;

    const user2Data = { name: 'User2', surname: 'Test', email: 'user2@test.com', password: 'Pass123!' };
    const reg2 = await request(app).post('/api/users/register').send(user2Data);
    const tokenRecord2 = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: reg2.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecord2.token });

    const firstShare = await request(app).post(`/api/shopping-lists/${list.id}/share`).set('Authorization', `Bearer ${jwt}`).send({ email: 'user2@test.com' });
    expect(firstShare.status).toBe(200);

    const secondShare = await request(app).post(`/api/shopping-lists/${list.id}/share`).set('Authorization', `Bearer ${jwt}`).send({ email: 'user2@test.com' });
    expect(secondShare.status).toBe(409);
    expect(secondShare.body.message).toBe('Already shared');
  });
});
