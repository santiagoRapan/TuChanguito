import { testDataSource } from './setup';
import * as UserService from '../services/user.service';
import { Mailer } from '../services/email.service';
import { User } from '../entities/user';
import { UserVerificationToken } from '../entities/userVerificationToken';
import { UserPasswordRecoveryToken } from '../entities/userPasswordRecoveryToken';

describe('User Service - Unit tests', () => {
  let mailer: Mailer & { sendEmail: jest.Mock };

  beforeAll(async () => {
    // testDataSource is initialized in setup.ts
  });

  beforeEach(async () => {
    const repoUser = testDataSource.getRepository(User);
    const repoVer = testDataSource.getRepository(UserVerificationToken);
    const repoRec = testDataSource.getRepository(UserPasswordRecoveryToken);
    await repoRec.clear();
    await repoVer.clear();
    await repoUser.clear();

    mailer = new Mailer({} as any) as any;
    mailer.sendEmail = jest.fn();
  });

  it('should create a new user and send verification email', async () => {
    const userData = {
      name: 'Unit',
      surname: 'Tester',
      email: 'unit@test.com',
      password: 'Pass123!'
    } as any;

    const result = await UserService.createNewUser(userData, mailer);
    expect(result).toBeDefined();
    expect(result.id).toBeGreaterThan(0);
    expect(mailer.sendEmail).toHaveBeenCalled();

    const saved = await testDataSource.getRepository(User).findOne({ where: { email: 'unit@test.com' } });
    expect(saved).toBeDefined();
  });

  it('should verify a user with valid token', async () => {
    const userData = {
      name: 'Verify',
      surname: 'Tester',
      email: 'verify@test.com',
      password: 'Pass123!'
    } as any;
    const user = await UserService.createNewUser(userData, mailer);
    
    const tokenRecord = await testDataSource.getRepository(UserVerificationToken).findOne({ 
      where: { user: { id: user.id } } 
    });
    const verificationToken = tokenRecord!.token;

    const verified = await UserService.verifyUser(verificationToken);
    expect(verified).toBeDefined();
    const dbUser = await testDataSource.getRepository(User).findOne({ where: { id: (verified as any).id } });
    expect(dbUser?.isVerified).toBe(true);
  });

  it('should login a verified user and return a token', async () => {
    const userData = {
      name: 'Login',
      surname: 'Tester',
      email: 'login@test.com',
      password: 'Pass123!'
    } as any;
    const user = await UserService.createNewUser(userData, mailer);
    
    const tokenRecord = await testDataSource.getRepository(UserVerificationToken).findOne({ 
      where: { user: { id: user.id } } 
    });
    await UserService.verifyUser(tokenRecord!.token);

    const token = await UserService.createNewUserToken({ email: 'login@test.com', password: 'Pass123!' });
    expect(typeof token).toBe('string');
  });

  it('should send password recovery email and reset password', async () => {
    const userData = {
      name: 'Recover',
      surname: 'Tester',
      email: 'recover@test.com',
      password: 'OldPass1'
    } as any;
    const user = await UserService.createNewUser(userData, mailer);
    
    const tokenRecord = await testDataSource.getRepository(UserVerificationToken).findOne({ 
      where: { user: { id: user.id } } 
    });
    await UserService.verifyUser(tokenRecord!.token);

    const sent = await UserService.sendPasswordRecoveryEmail('recover@test.com', mailer);
    expect(sent).toBe(true);
    expect(mailer.sendEmail).toHaveBeenCalled();

    const tokenRepo = testDataSource.getRepository(UserPasswordRecoveryToken);
    const tokenRows = await tokenRepo.find({ relations: ['user'] });
    const tokenRow = tokenRows[0];
    expect(tokenRow).toBeDefined();

    await UserService.resetUserPassword({ code: tokenRow!.token, password: 'NewPass1' } as any);

    const token = await UserService.createNewUserToken({ email: 'recover@test.com', password: 'NewPass1' });
    expect(typeof token).toBe('string');
  });

  it('should change password for verified user', async () => {
    const userData = {
      name: 'Change',
      surname: 'Tester',
      email: 'change@test.com',
      password: 'Cpass1'
    } as any;
    const user = await UserService.createNewUser(userData, mailer);
    const savedUser = await testDataSource.getRepository(User).findOne({ where: { email: 'change@test.com' } });
    
    const tokenRecord = await testDataSource.getRepository(UserVerificationToken).findOne({ 
      where: { user: { id: user.id } } 
    });
    await UserService.verifyUser(tokenRecord!.token);

    await UserService.changePassword(savedUser!.id, 'Cpass1', 'Cpass2');

    const token = await UserService.createNewUserToken({ email: 'change@test.com', password: 'Cpass2' });
    expect(typeof token).toBe('string');
  });

  it('should update user profile', async () => {
    const userData = {
      name: 'Update',
      surname: 'Tester',
      email: 'update@test.com',
      password: 'UpPass1'
    } as any;
    const user = await UserService.createNewUser(userData, mailer);
    const savedUser = await testDataSource.getRepository(User).findOne({ where: { email: 'update@test.com' } });
    
    const tokenRecord = await testDataSource.getRepository(UserVerificationToken).findOne({ 
      where: { user: { id: user.id } } 
    });
    await UserService.verifyUser(tokenRecord!.token);

    const updated = await UserService.updateUserProfile(savedUser!.id, 'NewName', 'NewSurname', { foo: 'bar' });
    expect((updated as any).name).toBe('NewName');
    expect((updated as any).metadata).toBeDefined();
  });
});
