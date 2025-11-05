import request from 'supertest';
import { app } from '../app';
import { testDataSource } from './setup';

describe('Pantry API - Integration tests', () => {
  let jwt: string;

  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository('product').clear();
    await testDataSource.getRepository('category').clear();
    await testDataSource.getRepository('pantry').clear();
    await testDataSource.getRepository('user').clear();
    await testDataSource.getRepository('user_verification_token').clear();

    const reg = await request(app).post('/api/users/register').send({ name: 'PantryInt', surname: 'Tester', email: 'pantryint@test.com', password: 'Pass123!' });
    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: reg.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecord.token });
    const login = await request(app).post('/api/users/login').send({ email: 'pantryint@test.com', password: 'Pass123!' });
    jwt = login.body.token;
  });

  it('should create, fetch, update and delete a pantry via API', async () => {
    const create = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwt}`).send({ name: 'HomePantry' });
    expect(create.status).toBe(201);
    const pantry = create.body || create.body.pantry || create.body;
    expect(pantry).toBeDefined();

    const list = await request(app).get('/api/pantries').set('Authorization', `Bearer ${jwt}`);
    expect(list.status).toBe(200);
    expect(Array.isArray(list.body.data)).toBe(true);
    expect(list.body.pagination).toBeDefined();

    const getRes = await request(app).get(`/api/pantries/${pantry.id}`).set('Authorization', `Bearer ${jwt}`);
    expect(getRes.status).toBe(200);

    const updateRes = await request(app).put(`/api/pantries/${pantry.id}`).set('Authorization', `Bearer ${jwt}`).send({ name: 'UpdatedPantry' });
    expect(updateRes.status).toBe(200);
    expect(updateRes.body.name || updateRes.body.pantry?.name).toBeDefined();

    const delRes = await request(app).delete(`/api/pantries/${pantry.id}`).set('Authorization', `Bearer ${jwt}`);
    expect(delRes.status).toBe(200);
  });

  it('should share and revoke pantry via API', async () => {
    const regA = await request(app).post('/api/users/register').send({ name: 'OwnerA', surname: 'A', email: 'ownera@example.com', password: 'Pass123!' });
    const tokenRecordA = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: regA.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecordA.token });
    const loginA = await request(app).post('/api/users/login').send({ email: 'ownera@example.com', password: 'Pass123!' });
    const jwtA = loginA.body.token;

    const regB = await request(app).post('/api/users/register').send({ name: 'UserB', surname: 'B', email: 'userb@example.com', password: 'Pass123!' });
    const tokenRecordB = await testDataSource.getRepository('user_verification_token').findOne({ where: { user: { id: regB.body.id } } });
    await request(app).post('/api/users/verify-account').send({ code: tokenRecordB.token });

    const create = await request(app).post('/api/pantries').set('Authorization', `Bearer ${jwtA}`).send({ name: 'SharedPantryAPI' });
    expect(create.status).toBe(201);
    const pantry = create.body || create.body.pantry || create.body;

    const share = await request(app).post(`/api/pantries/${pantry.id}/share`).set('Authorization', `Bearer ${jwtA}`).send({ email: 'userb@example.com' });
    expect(share.status).toBe(200);

    const shared = await request(app).get(`/api/pantries/${pantry.id}/shared-users`).set('Authorization', `Bearer ${jwtA}`);
    expect(shared.status).toBe(200);
    expect(Array.isArray(shared.body)).toBe(true);

    const revoke = await request(app).delete(`/api/pantries/${pantry.id}/share/${shared.body[0].id}`).set('Authorization', `Bearer ${jwtA}`);
    expect([200,204,200].includes(revoke.status)).toBeTruthy();
  });
});
