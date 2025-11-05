import request from 'supertest';
import { app } from '../app';
import { testDataSource } from './setup';

describe('Category API - Integration tests', () => {
  let jwt: string;

  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository('category').clear();
    await testDataSource.getRepository('user').clear();
    await testDataSource.getRepository('user_verification_token').clear();

    const reg = await request(app)
      .post('/api/users/register')
      .send({ name: 'CatInt', surname: 'Tester', email: 'catint@test.com', password: 'Pass123!' });

    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ 
      where: { user: { id: reg.body.id } } 
    });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecord.token });

    const login = await request(app).post('/api/users/login').send({ email: 'catint@test.com', password: 'Pass123!' });
    jwt = login.body.token;
  });

  it('should create a category', async () => {
    const res = await request(app)
      .post('/api/categories')
      .set('Authorization', `Bearer ${jwt}`)
      .send({ name: 'IntegrationCategory' });

    expect(res.status).toBe(201);
    expect(res.body).toBeDefined();
    expect(res.body.category || res.body).toBeDefined();
  });

  it('should list categories', async () => {
    await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'C1' });
    await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'C2' });

    const res = await request(app).get('/api/categories').set('Authorization', `Bearer ${jwt}`);
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body.data)).toBe(true);
    expect(res.body.pagination).toBeDefined();
    expect(res.body.data.length).toBeGreaterThanOrEqual(2);
  });

  it('should get, update and delete a category', async () => {
    const create = await request(app).post('/api/categories').set('Authorization', `Bearer ${jwt}`).send({ name: 'ToManage' });
    const created = create.body.category || create.body;
    const id = created.id || created.id;

    const getRes = await request(app).get(`/api/categories/${id}`).set('Authorization', `Bearer ${jwt}`);
    expect(getRes.status).toBe(200);
    expect(getRes.body).toBeDefined();

    const updateRes = await request(app).put(`/api/categories/${id}`).set('Authorization', `Bearer ${jwt}`).send({ name: 'UpdatedName' });
    expect(updateRes.status).toBe(200);
    expect(updateRes.body).toBeDefined();

    const deleteRes = await request(app).delete(`/api/categories/${id}`).set('Authorization', `Bearer ${jwt}`);
    expect(deleteRes.status).toBe(200);
  });
});

