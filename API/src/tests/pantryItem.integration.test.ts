import request from 'supertest';
import { app } from '../app';
import { testDataSource } from './setup';

describe('PantryItem API - Integration tests', () => {
  let jwt: string;

  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository('product').clear();
    await testDataSource.getRepository('category').clear();
    await testDataSource.getRepository('pantry').clear();
    await testDataSource.getRepository('pantry_item').clear();
    await testDataSource.getRepository('user').clear();
    await testDataSource.getRepository('user_verification_token').clear();

    const reg = await request(app).post('/api/users/register').send({ name: 'PIInt', surname: 'Tester', email: 'piint@test.com', password: 'Pass123!' });
    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: reg.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecord.token });
    const login = await request(app).post('/api/users/login').send({ email: 'piint@test.com', password: 'Pass123!' });
    jwt = login.body.token;
  });

  it('should add, list, update and delete pantry items through API', async () => {
    const pantryRes = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwt}`).send({ name: 'PI-Pantry' });
    const pantry = pantryRes.body || pantryRes.body.pantry || pantryRes.body;

    const catRes = await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'PI-Cat' });
    const category = catRes.body.category || catRes.body;

    const prodRes = await request(app).post('/api/products').set('Authorization', `Bearer ${jwt}`).send({ name: 'PI-Prod', category: { id: category.id } });
    const product = prodRes.body.product || prodRes.body;

    const add = await request(app).post(`/api/pantries/${pantry.id}/items`).set('Authorization', `Bearer ${jwt}`).send({ product: { id: product.id }, quantity: 2, unit: 'pcs' });
    expect(add.status).toBe(201);
    const item = add.body || add.body.item || add.body;

    const list = await request(app).get(`/api/pantries/${pantry.id}/items`).set('Authorization', `Bearer ${jwt}`);
    expect(list.status).toBe(200);
    expect(Array.isArray(list.body.data)).toBe(true);
    expect(list.body.pagination).toBeDefined();

    const upd = await request(app).put(`/api/pantries/${pantry.id}/items/${item.id}`).set('Authorization', `Bearer ${jwt}`).send({ quantity: 5, unit: "kg" });
    expect(upd.status).toBe(200);

    const del = await request(app).delete(`/api/pantries/${pantry.id}/items/${item.id}`).set('Authorization', `Bearer ${jwt}`);
    expect(del.status).toBe(200);
  });

});

