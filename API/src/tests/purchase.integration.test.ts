import request from 'supertest';
import { app } from '../app';
import { testDataSource } from './setup';

describe('Purchase API - Integration tests', () => {
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

    const reg = await request(app).post('/api/users/register').send({ name: 'PInt', surname: 'Tester', email: 'pint@test.com', password: 'Pass123!' });
    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: reg.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecord.token });
    const login = await request(app).post('/api/users/login').send({ email: 'pint@test.com', password: 'Pass123!' });
    jwt = login.body.token;
  });

  it('should create a purchase via API and restore it', async () => {
    const pantryRes = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwt}`).send({ name: 'P-Pantry' });
    const pantry = pantryRes.body || pantryRes.body.pantry || pantryRes.body;

    const catRes = await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'P-Cat' });
    const category = catRes.body.category || catRes.body;

    const prodRes = await request(app).post('/api/products').set('Authorization', `Bearer ${jwt}`).send({ name: 'P-Prod', category: { id: category.id } });
    const product = prodRes.body.product || prodRes.body;

    const create = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send({ name: 'P-List Integration Test', description: 'desc', recurring: false });
    expect(create.status).toBe(201);
    const list = create.body || create.body.list || create.body;

    const addItem = await request(app).post(`/api/shopping-lists/${list.id}/items`).set('Authorization', `Bearer ${jwt}`).send({ product: { id: product.id }, quantity: 2, unit: 'pcs' });
    expect(addItem.status).toBe(201);
    const item = addItem.body.item || addItem.body;

    const patch = await request(app).patch(`/api/shopping-lists/${list.id}/items/${item.id}`).set('Authorization', `Bearer ${jwt}`).send({ purchased: true });
    expect(patch.status).toBe(200);

    const purchase = await request(app).post(`/api/shopping-lists/${list.id}/purchase`).set('Authorization', `Bearer ${jwt}`).send({ metadata: { note: 'bought' } });
    expect(purchase.status).toBe(201);
    const purchased = purchase.body || purchase.body.purchase || purchase.body;

    const getOne = await request(app).get(`/api/purchases/${purchased.id}`).set('Authorization', `Bearer ${jwt}`);
    expect(getOne.status).toBe(200);

    const restore = await request(app).post(`/api/purchases/${purchased.id}/restore`).set('Authorization', `Bearer ${jwt}`);
    expect(restore.status).toBe(201);
    const restored = restore.body || restore.body.list || restore.body;
    expect(restored).toBeDefined();
  });

  it('should restore purchase with unique name when list already exists', async () => {
    const pantryRes = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwt}`).send({ name: 'P-Pantry-2' });
    const pantry = pantryRes.body || pantryRes.body.pantry || pantryRes.body;

    const catRes = await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'P-Cat-2' });
    const category = catRes.body.category || catRes.body;

    const prodRes = await request(app).post('/api/products').set('Authorization', `Bearer ${jwt}`).send({ name: 'P-Prod-2', category: { id: category.id } });
    const product = prodRes.body.product || prodRes.body;

    const create1 = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send({ name: 'Test List', description: 'desc', recurring: false });
    expect(create1.status).toBe(201);
    const list1 = create1.body || create1.body.list || create1.body;

    const addItem1 = await request(app).post(`/api/shopping-lists/${list1.id}/items`).set('Authorization', `Bearer ${jwt}`).send({ product: { id: product.id }, quantity: 2, unit: 'pcs' });
    expect(addItem1.status).toBe(201);

    const patch1 = await request(app).patch(`/api/shopping-lists/${list1.id}/items/${addItem1.body.item.id}`).set('Authorization', `Bearer ${jwt}`).send({ purchased: true });
    expect(patch1.status).toBe(200);

    const purchase1 = await request(app).post(`/api/shopping-lists/${list1.id}/purchase`).set('Authorization', `Bearer ${jwt}`).send({ metadata: { note: 'bought' } });
    expect(purchase1.status).toBe(201);

    const purchases = await request(app).get('/api/purchases').set('Authorization', `Bearer ${jwt}`);
    expect(purchases.status).toBe(200);
    expect(purchases.body.data.length).toBeGreaterThan(0);
    const purchaseRecord = purchases.body.data[0];

    const restore1 = await request(app).post(`/api/purchases/${purchaseRecord.id}/restore`).set('Authorization', `Bearer ${jwt}`);
    expect(restore1.status).toBe(201);
    const restored1 = restore1.body.list || restore1.body;
    expect(restored1.name).toBe('Test List (1)');
  });

  it('should restore purchase skipping items with deleted products', async () => {
    const pantryRes = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwt}`).send({ name: 'Test Pantry' });
    const pantry = pantryRes.body;

    const catRes = await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'Test Category' });
    const category = catRes.body.category || catRes.body;

    const prod1Res = await request(app).post('/api/products').set('Authorization', `Bearer ${jwt}`).send({ name: 'Product 1', pantry_id: pantry.id, category_id: category.id });
    const product1 = prod1Res.body.product || prod1Res.body;

    const prod2Res = await request(app).post('/api/products').set('Authorization', `Bearer ${jwt}`).send({ name: 'Product 2', pantry_id: pantry.id, category_id: category.id });
    const product2 = prod2Res.body.product || prod2Res.body;

    const listRes = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send({ name: 'Test List', description: 'Test', recurring: false });
    const list = listRes.body.list || listRes.body;

    const addItem1 = await request(app).post(`/api/shopping-lists/${list.id}/items`).set('Authorization', `Bearer ${jwt}`).send({ product: { id: product1.id }, quantity: 2, unit: 'pcs' });
    expect(addItem1.status).toBe(201);

    const addItem2 = await request(app).post(`/api/shopping-lists/${list.id}/items`).set('Authorization', `Bearer ${jwt}`).send({ product: { id: product2.id }, quantity: 1, unit: 'kg' });
    expect(addItem2.status).toBe(201);

    await request(app).patch(`/api/shopping-lists/${list.id}/items/${addItem1.body.item.id}`).set('Authorization', `Bearer ${jwt}`).send({ purchased: true });
    await request(app).patch(`/api/shopping-lists/${list.id}/items/${addItem2.body.item.id}`).set('Authorization', `Bearer ${jwt}`).send({ purchased: true });

    const purchase = await request(app).post(`/api/shopping-lists/${list.id}/purchase`).set('Authorization', `Bearer ${jwt}`).send({ metadata: { note: 'bought' } });
    expect(purchase.status).toBe(201);

    const deleteProd = await request(app).delete(`/api/products/${product1.id}`).set('Authorization', `Bearer ${jwt}`);
    expect(deleteProd.status).toBe(200);

    const purchases = await request(app).get('/api/purchases').set('Authorization', `Bearer ${jwt}`);
    const purchaseRecord = purchases.body.data[0];

    const restore = await request(app).post(`/api/purchases/${purchaseRecord.id}/restore`).set('Authorization', `Bearer ${jwt}`);
    expect(restore.status).toBe(201);
    
    const restoredList = restore.body.list || restore.body;
    expect(restoredList.name).toBe('Test List (1)');

    const items = await request(app).get(`/api/shopping-lists/${restoredList.id}/items`).set('Authorization', `Bearer ${jwt}`);
    expect(items.status).toBe(200);
    expect(items.body.data.length).toBe(1);
    expect(items.body.data[0].product.name).toBe('Product 2');
  });
});
