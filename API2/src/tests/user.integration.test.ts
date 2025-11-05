import request from 'supertest';
import { app } from '../app';
import { testDataSource } from './setup';

describe('User API - Integration tests', () => {
  beforeAll(async () => {
    // testDataSource initialized in setup
  });

  beforeEach(async () => {
    await testDataSource.getRepository('user_verification_token').clear();
    await testDataSource.getRepository('user_password_recovery_token').clear();
    await testDataSource.getRepository('user').clear();
  });

  it('POST /api/users/register -> should register and return user', async () => {
    const res = await request(app)
      .post('/api/users/register')
      .send({
        name: 'Int',
        surname: 'Tester',
        email: 'int@test.com',
        password: 'Pass123!'
      });

    expect(res.status).toBe(201);
    expect(res.body).toBeDefined();
    expect(res.body.id).toBeDefined();
    expect(res.body.name).toBe('Int');
    expect(res.body.email).toBe('int@test.com');
  });

  it('POST /api/users/verify-account -> should verify user with token', async () => {
    const reg = await request(app)
      .post('/api/users/register')
      .send({ name: 'V', surname: 'Test', email: 'vtest@test.com', password: 'Pass123!' });

    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ 
      where: { user: { id: reg.body.id } } 
    });
    expect(tokenRecord).toBeDefined();
    const token = tokenRecord.token;

    const verify = await request(app)
      .post('/api/users/verify-account')
      .send({ code: token });

    expect(verify.status).toBe(200);
  });

  it('POST /api/users/login -> should login and return JWT token', async () => {
    const reg = await request(app)
      .post('/api/users/register')
      .send({ name: 'L', surname: 'Test', email: 'loginint@test.com', password: 'Pass123!' });

    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ 
      where: { user: { id: reg.body.id } } 
    });
    const token = tokenRecord.token;
    await request(app).post('/api/users/verify-account').send({ code: token });

    const login = await request(app)
      .post('/api/users/login')
      .send({ email: 'loginint@test.com', password: 'Pass123!' });

    expect(login.status).toBe(200);
    expect(login.body.token).toBeDefined();
  });

  it('GET /api/users/profile -> should return profile when authenticated', async () => {
    const reg = await request(app)
      .post('/api/users/register')
      .send({ name: 'P', surname: 'Test', email: 'profile@test.com', password: 'Pass123!' });

    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ 
      where: { user: { id: reg.body.id } } 
    });
    const token = tokenRecord.token;
    await request(app).post('/api/users/verify-account').send({ code: token });

    const login = await request(app)
      .post('/api/users/login')
      .send({ email: 'profile@test.com', password: 'Pass123!' });

    const jwt = login.body.token;
    const profile = await request(app).get('/api/users/profile').set('Authorization', `Bearer ${jwt}`);
    expect(profile.status).toBe(200);
    expect(profile.body).toBeDefined();
    expect(profile.body).toHaveProperty('id');
  });

  it('POST /api/users/register -> should return 409 when email already exists', async () => {
    await request(app)
      .post('/api/users/register')
      .send({
        name: 'First',
        surname: 'User',
        email: 'duplicate@test.com',
        password: 'Pass123!'
      });

    const res = await request(app)
      .post('/api/users/register')
      .send({
        name: 'Second',
        surname: 'User',
        email: 'duplicate@test.com',
        password: 'Pass123!'
      });

    expect(res.status).toBe(409);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toBe('Email already registered');
  });

  it('POST /api/users/send-verification -> should return 409 when account is already verified', async () => {
    const reg = await request(app)
      .post('/api/users/register')
      .send({
        name: 'Verified',
        surname: 'User',
        email: 'verified@test.com',
        password: 'Pass123!'
      });

    const tokenRecord = await testDataSource.getRepository('user_verification_token').findOne({ 
      where: { user: { id: reg.body.id } } 
    });
    
    await request(app)
      .post('/api/users/verify-account')
      .send({ code: tokenRecord.token });

    const res = await request(app)
      .post('/api/users/send-verification')
      .query({ email: 'verified@test.com' });

    expect(res.status).toBe(409);
  });
});
