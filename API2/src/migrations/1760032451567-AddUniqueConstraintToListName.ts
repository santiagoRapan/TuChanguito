import { MigrationInterface, QueryRunner } from "typeorm";

export class AddUniqueConstraintToListName1760032451567 implements MigrationInterface {
    name = 'AddUniqueConstraintToListName1760032451567'

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`CREATE TABLE "temporary_list" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "description" varchar NOT NULL, "metadata" text, "recurring" boolean NOT NULL DEFAULT (0), "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "lastPurchasedAt" date, "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "unique_list_name_per_owner" UNIQUE ("name", "ownerId"), CONSTRAINT "FK_f0f290e54e6663b786fe5a8134f" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "temporary_list"("id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId" FROM "list"`);
        await queryRunner.query(`DROP TABLE "list"`);
        await queryRunner.query(`ALTER TABLE "temporary_list" RENAME TO "list"`);
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`ALTER TABLE "list" RENAME TO "temporary_list"`);
        await queryRunner.query(`CREATE TABLE "list" ("id" integer PRIMARY KEY AUTOINCREMENT NOT NULL, "name" varchar NOT NULL, "description" varchar NOT NULL, "metadata" text, "recurring" boolean NOT NULL DEFAULT (0), "updatedAt" datetime NOT NULL DEFAULT (datetime('now')), "lastPurchasedAt" date, "createdAt" datetime NOT NULL DEFAULT (datetime('now')), "deletedAt" datetime, "ownerId" integer, CONSTRAINT "FK_f0f290e54e6663b786fe5a8134f" FOREIGN KEY ("ownerId") REFERENCES "user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION)`);
        await queryRunner.query(`INSERT INTO "list"("id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId") SELECT "id", "name", "description", "metadata", "recurring", "updatedAt", "lastPurchasedAt", "createdAt", "deletedAt", "ownerId" FROM "temporary_list"`);
        await queryRunner.query(`DROP TABLE "temporary_list"`);
    }

}
