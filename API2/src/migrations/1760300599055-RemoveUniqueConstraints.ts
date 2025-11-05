import { MigrationInterface, QueryRunner } from "typeorm";

export class RemoveUniqueConstraints1760300599055 implements MigrationInterface {
    name = 'RemoveUniqueConstraints1760300599055'

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`CREATE TABLE "temporary_pantry" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "metadata" text, "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "FK_5cef695d90fd65bc4ce1a029706" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "temporary_pantry"("id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId" FROM "pantry"`);
        await queryRunner.query(`DROP TABLE "pantry"`);
        await queryRunner.query(`ALTER TABLE "temporary_pantry" RENAME TO "pantry"`);
        await queryRunner.query(`CREATE TABLE "temporary_product" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "metadata" text, "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, "categoryId" integer, "pantryId" integer, CONSTRAINT "FK_cbb5d890de1519efa20c42bcd52" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION, CONSTRAINT "FK_ff0c0301a95e517153df97f6812" FOREIGN KEY ("categoryId") REFERENCES "category" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION, CONSTRAINT "FK_3b91dbd9500d8e578dd5373d67b" FOREIGN KEY ("pantryId") REFERENCES "pantry" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "temporary_product"("id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId", "categoryId", "pantryId") SELECT "id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId", "categoryId", "pantryId" FROM "product"`);
        await queryRunner.query(`DROP TABLE "product"`);
        await queryRunner.query(`ALTER TABLE "temporary_product" RENAME TO "product"`);
        await queryRunner.query(`CREATE TABLE "temporary_category" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "metadata" text, "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "FK_ffcf79002e1738147305ea57664" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "temporary_category"("id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId" FROM "category"`);
        await queryRunner.query(`DROP TABLE "category"`);
        await queryRunner.query(`ALTER TABLE "temporary_category" RENAME TO "category"`);
        await queryRunner.query(`CREATE TABLE "temporary_list" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "description" varchar NOT NULL, "metadata" text, "recurring" boolean NOT NULL DEFAULT (0), "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "lastPurchasedAt" date, "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "FK_f0f290e54e6663b786fe5a8134f" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "temporary_list"("id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId" FROM "list"`);
        await queryRunner.query(`DROP TABLE "list"`);
        await queryRunner.query(`ALTER TABLE "temporary_list" RENAME TO "list"`);
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`ALTER TABLE "list" RENAME TO "temporary_list"`);
        await queryRunner.query(`CREATE TABLE "list" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "description" varchar NOT NULL, "metadata" text, "recurring" boolean NOT NULL DEFAULT (0), "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "lastPurchasedAt" date, "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "unique_list_name_per_owner" UNIQUE ("name", "ownerId"), CONSTRAINT "FK_f0f290e54e6663b786fe5a8134f" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "list"("id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId" FROM "temporary_list"`);
        await queryRunner.query(`DROP TABLE "temporary_list"`);
        await queryRunner.query(`ALTER TABLE "category" RENAME TO "temporary_category"`);
        await queryRunner.query(`CREATE TABLE "category" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "metadata" text, "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "UQ_f8c7444ff257db87860b7301704" UNIQUE ("name", "ownerId"), CONSTRAINT "FK_ffcf79002e1738147305ea57664" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "category"("id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId" FROM "temporary_category"`);
        await queryRunner.query(`DROP TABLE "temporary_category"`);
        await queryRunner.query(`ALTER TABLE "product" RENAME TO "temporary_product"`);
        await queryRunner.query(`CREATE TABLE "product" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "metadata" text, "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, "categoryId" integer, "pantryId" integer, CONSTRAINT "UQ_a9f20c159b3b6228ef013e2eea0" UNIQUE ("name", "ownerId"), CONSTRAINT "FK_cbb5d890de1519efa20c42bcd52" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION, CONSTRAINT "FK_ff0c0301a95e517153df97f6812" FOREIGN KEY ("categoryId") REFERENCES "category" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION, CONSTRAINT "FK_3b91dbd9500d8e578dd5373d67b" FOREIGN KEY ("pantryId") REFERENCES "pantry" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "product"("id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId", "categoryId", "pantryId") SELECT "id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId", "categoryId", "pantryId" FROM "temporary_product"`);
        await queryRunner.query(`DROP TABLE "temporary_product"`);
        await queryRunner.query(`ALTER TABLE "pantry" RENAME TO "temporary_pantry"`);
        await queryRunner.query(`CREATE TABLE "pantry" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "metadata" text, "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "UQ_a423f1a4493964e499f00bb81d0" UNIQUE ("name", "ownerId"), CONSTRAINT "FK_5cef695d90fd65bc4ce1a029706" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "pantry"("id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "metadata", "updatedAt", "createdAt", "deletedAt", "ownerId" FROM "temporary_pantry"`);
        await queryRunner.query(`DROP TABLE "temporary_pantry"`);
    }

}
