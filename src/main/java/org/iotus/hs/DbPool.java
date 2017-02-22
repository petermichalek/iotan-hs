package org.iotus.hs;

/**
 * Database pool.
 */
public class DbPool extends ObjectPool<IMDatabase> {

    public DbPool() {
    }

    @Override
    protected IMDatabase create() {
        return new IMDatabase();
    }

    @Override
    public void expire(IMDatabase o) {
        // TODO: close db object (close db connection)
    }

    @Override
    public boolean validate(IMDatabase o) {
        return true;
    }
}