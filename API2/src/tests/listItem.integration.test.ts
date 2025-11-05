import request from 'supertest';
import { app } from '../app';
import { testDataSource } from './setup';

describe('ListItem API - Integration tests', () => {
  let jwt: string;

  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository('product').clear();
    await testDataSource.getRepository('category').clear();
    await testDataSource.getRepository('pantry').clear();
    await testDataSource.getRepository('list').clear();
    await testDataSource.getRepository('list_item').clear();
    await testDataSource.getRepository('user').clear();
    await testDataSource.getRepository('user_verification_token').clear();

    const reg = await request(app).post('/api/users/register').send({ name: 'LIInt', surname: 'Tester', email: 'liint@test.com', password: 'Pass123!' });
    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: reg.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecord.token });
    const login = await request(app).post('/api/users/login').send({ email: 'liint@test.com', password: 'Pass123!' });
    jwt = login.body.token;
  });

  it('should add, list, update, toggle and delete list items through API', async () => {
    const pantryRes = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwt}`).send({ name: 'LI-Pantry' });
    const pantry = pantryRes.body || pantryRes.body.pantry || pantryRes.body;

    const catRes = await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'LI-Cat' });
    const category = catRes.body.category || catRes.body;

    const prodRes = await request(app).post('/api/products').set('Authorization', `Bearer ${jwt}`).send({ name: 'LI-Prod', pantry_id: pantry.id, category_id: category.id });
    const product = prodRes.body.product || prodRes.body;

    const create = await request(app).post('/api/shopping-lists').set('Authorization', `Bearer ${jwt}`).send({ name: 'LI-List', description: 'desc', recurring: false });
    expect(create.status).toBe(201);
    const list = create.body || create.body.list || create.body;

    const addItem = await request(app).post(`/api/shopping-lists/${list.id}/items`).set('Authorization', `Bearer ${jwt}`).send({ product: { id: product.id }, quantity: 1, unit: 'pcs' });
    expect(addItem.status).toBe(201);
    const item = addItem.body.item || addItem.body;

    const getItems = await request(app).get(`/api/shopping-lists/${list.id}/items`).set('Authorization', `Bearer ${jwt}`);
    expect(getItems.status).toBe(200);
    const items = getItems.body.data || [];
    expect(Array.isArray(items)).toBe(true);

    const update = await request(app).put(`/api/shopping-lists/${list.id}/items/${item.id}`).set('Authorization', `Bearer ${jwt}`).send({ quantity: 5, unit: "kg" });
    expect(update.status).toBe(200);

    const toggle = await request(app).patch(`/api/shopping-lists/${list.id}/items/${item.id}`).set('Authorization', `Bearer ${jwt}`).send({ purchased: true });
    expect(toggle.status).toBe(200);

    const del = await request(app).delete(`/api/shopping-lists/${list.id}/items/${item.id}`).set('Authorization', `Bearer ${jwt}`);
    expect(del.status).toBe(200);
  });
});
