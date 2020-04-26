/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the LGPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at  http://www.gnu.org/licenses/lgpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jn.sqlhelper.dialect;

import com.jn.langx.annotation.Name;
import com.jn.langx.text.StringTemplates;
import com.jn.langx.util.Strings;
import com.jn.langx.util.collection.Pipeline;
import com.jn.langx.util.function.Predicate;
import com.jn.langx.util.reflect.Reflects;
import com.jn.langx.util.struct.Holder;
import com.jn.sqlhelper.dialect.annotation.Driver;
import com.jn.sqlhelper.dialect.internal.*;
import com.jn.sqlhelper.dialect.sqlparser.SqlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class DialectRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DialectRegistry.class);
    private static final Map<String, Dialect> nameToDialectMap = new TreeMap<String, Dialect>();
    private static final Map<String, String> classNameToNameMap = new TreeMap<String, String>();
    // key:DatabaseMetaData.getProduceName() + getDriver();
    private static final Map<String, Holder<Dialect>> dbToDialectMap = new HashMap<String, Holder<Dialect>>();
    private static final Properties vendorDatabaseIdMappings = new Properties();

    static {
        loadDatabaseIdMappings();
        registerBuiltinDialects();
        loadCustomDialects();
    }

    private DialectRegistry() {
    }

    private static final DialectRegistry registry = new DialectRegistry();

    public static DialectRegistry getInstance() {
        return registry;
    }

    public Collection<Dialect> getDialects() {
        return nameToDialectMap.values();
    }

    public Dialect getDialectByClassName(final String className) {
        final String dialectName = (String) DialectRegistry.classNameToNameMap.get(className);
        if (dialectName != null) {
            return this.getDialectByName(dialectName);
        }
        return null;
    }

    public Dialect getDialectByName(final String databaseId) {
        return DialectRegistry.nameToDialectMap.get(databaseId);
    }

    private static String databaseIdStringLowerCase(DatabaseMetaData databaseMetaData) {
        return databaseIdString(databaseMetaData).toLowerCase();
    }

    private static String databaseIdString(DatabaseMetaData databaseMetaData) {
        try {
            return databaseMetaData.getDatabaseProductName();
        } catch (SQLException ex) {
            try {
                return databaseMetaData.getDriverName().toLowerCase() + " version: " + databaseMetaData.getDriverVersion().toLowerCase();
            } catch (SQLException ex1) {
                // ignore it
            }
        }
        try {
            return databaseMetaData.getURL();
        } catch (SQLException ex) {
            logger.warn(ex.getMessage(), ex);
        }
        return databaseMetaData.getClass().getCanonicalName();
    }

    public Dialect getDialectByDatabaseMetadata(final DatabaseMetaData databaseMetaData) {
        Dialect dialect = null;
        if (databaseMetaData != null) {
            String databaseIdString = databaseIdStringLowerCase(databaseMetaData);
            try {
                dialect = ((Holder<Dialect>) DialectRegistry.dbToDialectMap.get(databaseIdString)).get();
            } catch (NullPointerException ex) {
                // ignore
            }
            if (dialect == null) {
                Enumeration<String> keys = (Enumeration<String>) vendorDatabaseIdMappings.propertyNames();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    if (databaseIdString.contains(key.toLowerCase())) {
                        dialect = getDialectByName(vendorDatabaseIdMappings.getProperty(key));
                        if (dialect != null) {
                            dbToDialectMap.put(databaseIdString, new Holder<Dialect>(dialect));
                            break;
                        }
                    }
                }
            }
        }
        return dialect;
    }

    private static void loadCustomDialects() {
        loadCustomDialects(DialectRegistry.class.getClassLoader());
    }

    public static void loadCustomDialects(ClassLoader classLoader) {
        ServiceLoader<AbstractDialect> serviceLoader = ServiceLoader.load(AbstractDialect.class, classLoader);
        for (AbstractDialect dialect : serviceLoader) {
            registerDialectByClass(dialect.getClass(), dialect);
        }
    }

    private static void registerBuiltinDialects() {
        logger.info("Start to register builtin dialects");

        final Class<? extends Dialect>[] dialects = (Class<? extends Dialect>[]) new Class[]{

                AccessDialect.class,
                ActorDBDialect.class,
                AgensGraphDialect.class,
                AltibaseDialect.class,
                AntDBDialect.class,
                AuroraDialect.class,
                AzureDialect.class,

                BigObjectDialect.class,
                BrytlytDialect.class,

                CacheDialect.class,
                CirroDBDialect.class,
                CitusDialect.class,
                ClickHouseDialect.class,
                ClustrixDialect.class,
                CobolDialect.class,
                CockroachDialect.class,
                ComDB2Dialect.class,
                CovenantSQLDialect.class,
                CrateDialect.class,
                CTreeDialect.class,
                CubridDialect.class,

                DB2Dialect.class,
                DbfDialect.class,
                DerbyDialect.class,
                DmDialect.class,
                DorisDialect.class,
                DrillDialect.class,

                ElasticsearchDialect.class,
                EsgynDBDialect.class,

                FileMakerDialect.class,
                FirebirdDialect.class,

                GaussDbDialect.class,
                GBaseDialect.class,
                GoldenDBDialect.class,
                GreenplumDialect.class,

                H2Dialect.class,
                HANADialect.class,
                HawqDialect.class,
                HerdDBDialect.class,
                HhDbDialect.class,
                HighGoDialect.class,
                HiveDialect.class,
                HSQLDialect.class,

                IgniteDialect.class,
                ImpalaDialect.class,
                InformixDialect.class,
                IngresDialect.class,
                InterbaseDialect.class,
                IrisDialect.class,

                JDataStoreDialect.class,

                KarelDBDialect.class,
                KDBDialect.class,
                KingbaseDialect.class,
                KineticaDialect.class,
                KognitioDialect.class,

                LeanXcaleDialect.class,
                LinterDialect.class,

                MariaDBDialect.class,
                MaxComputeDialect.class,
                MaxDBDialect.class,
                MckoiDialect.class,
                MemSQLDialect.class,
                MimerSQLDialect.class,
                MonetDialect.class,
                MSQLDialect.class,
                MySQLDialect.class,

                Neo4jDialect.class,
                NetezzaDialect.class,
                NexusDBDialect.class,
                NuodbDialect.class,

                OBaseDialect.class,
                OmnisciDialect.class,
                OpenbaseDialect.class,
                OpenEdgeDialect.class,
                OracleDialect.class,
                OrientDBDialect.class,
                OscarDialect.class,

                ParadoxDialect.class,
                PerconaMysqlDialect.class,
                PhoenixDialect.class,
                PointbaseDialect.class,
                PostgreSQLDialect.class,
                PrestoDialect.class,

                RadonDBDialect.class,
                RaimaDialect.class,
                RBaseDialect.class,
                RDMSOS2200Dialect.class,
                RedshiftDialect.class,

                SadasDialect.class,
                SequoiaDBDialect.class,
                SinoDBDialect.class,
                SmallDialect.class,
                SnappyDataDialect.class,
                SnowflakeDialect.class,
                SpliceMachineDialect.class,
                SQLiteDialect.class,
                SQLServerDialect.class,
                SQReamDialect.class,

                TajoDialect.class,
                TeradataDialect.class,
                TiDBDialect.class,
                TimesTenDialect.class,
                TrafodionDialect.class,
                TransbaseDialect.class,

                UxDBDialect.class,

                ValentinaDialect.class,
                VerticaDialect.class,
                VirtuosoDialect.class,
                VistaDBDialect.class,
                VoltDBDialect.class,

                XtremeSQLDialect.class,

                YaacomoDialect.class,
                YugabyteDBDialect.class
        };

        for (Class<? extends Dialect> clazz : Arrays.asList(dialects)) {
            registerDialectByClass(clazz, null);
        }

        logger.info("Registered dialects: {}", nameToDialectMap.keySet());
    }

    private static void loadDatabaseIdMappings() {
        logger.info("Start to load database id mappings");
        InputStream inputStream = DialectRegistry.class.getResourceAsStream("/sqlhelper-dialect-databaseid.properties");
        if (inputStream != null) {
            try {
                vendorDatabaseIdMappings.load(inputStream);
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                try {
                    inputStream.close();
                } catch (Throwable ex) {
                    // Ignore it
                }
            }
        }
    }

    public static Properties getVendorDatabaseIdMappings() {
        return vendorDatabaseIdMappings;
    }

    public static void setDatabaseId(String keywordsInDriver, String databaseId) {
        vendorDatabaseIdMappings.setProperty(keywordsInDriver, databaseId);
    }

    /**
     * guess based productName, url, driver etc
     *
     * @return database id
     */
    public static String guessDatabaseId(String productName) {

        if (productName == null) {
            return null;
        }
        Iterator<Object> iter = (vendorDatabaseIdMappings.keySet().iterator());
        productName = productName.toLowerCase();
        while (iter.hasNext()) {
            String databaseId = iter.next().toString().toLowerCase();
            if (productName.contains(databaseId)) {
                return databaseId;
            }
        }
        return null;
    }

    public void registerDialectByClassName(final String className) throws ClassNotFoundException {
        this.registerDialect(null, className);
    }

    public void registerDialect(final String dialectName, final String className) throws ClassNotFoundException {
        final Class<? extends Dialect> clazz = loadDialectClass(className);
        try {
            final Dialect dialect = registerDialectByClass(clazz);
            if (!Strings.isBlank(dialectName) && dialect != null) {
                DialectRegistry.nameToDialectMap.put(dialectName, dialect);
            }
        } catch (Throwable ex) {
            DialectRegistry.logger.info(ex.getMessage(), ex);
        }
    }

    private static Class<? extends Dialect> loadDialectClass(final String className) throws ClassNotFoundException {
        return (Class<? extends Dialect>) loadImplClass(className, Dialect.class);
    }

    private static Class<? extends java.sql.Driver> loadDriverClass(final String className) throws ClassNotFoundException {
        return (Class<? extends java.sql.Driver>) loadImplClass(className, java.sql.Driver.class);
    }

    private static Class loadImplClass(final String className, final Class superClass) throws ClassNotFoundException {
        Class clazz = null;
        try {
            clazz = Class.forName(className, true, DialectRegistry.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // NOOP
        }
        if (clazz == null) {
            clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }
        if (superClass.isAssignableFrom(clazz)) {
            return clazz;
        }
        final String error = "Class " + Reflects.getFQNClassName(clazz) + " is not cast to " + Reflects.getFQNClassName(superClass);
        throw new ClassCastException(error);
    }

    private static Dialect registerDialectByClass(final Class<? extends Dialect> clazz) {
        return registerDialectByClass(clazz, null);
    }

    private static Dialect registerDialectByClass(final Class<? extends Dialect> clazz, Dialect dialect) {

        final Name nameAnno = (Name) Reflects.getAnnotation(clazz, Name.class);
        String name;
        if (nameAnno != null) {
            name = nameAnno.value();
            if (Strings.isBlank(name)) {
                throw new IllegalStateException("@Name is empty in class" + Reflects.getFQNClassName(clazz));
            }
        } else {
            final String simpleClassName = clazz.getSimpleName().toLowerCase();
            name = simpleClassName.replaceAll("dialect", "");
        }
        if (dialect == null) {
            final Driver driverAnno = (Driver) Reflects.getAnnotation(clazz, Driver.class);
            Class<? extends java.sql.Driver> driverClass = null;
            Constructor<? extends Dialect> driverConstructor = null;
            if (driverAnno != null) {
                final String driverClassName = driverAnno.value();
                if (Strings.isBlank(driverClassName)) {
                    throw new IllegalStateException("@Driver is empty in class" + Reflects.getFQNClassName(clazz));
                }
                try {
                    driverClass = loadDriverClass(driverClassName);
                    try {
                        driverConstructor = clazz.getDeclaredConstructor(java.sql.Driver.class);
                    } catch (Throwable ex) {
                        logger.info("Can't find the driver based constructor for dialect {}", (Object) name);
                    }
                } catch (Throwable ex) {
                    logger.info("Can't find driver class {} for {} dialect", (Object) driverClassName, (Object) name);
                }
            }
            if (driverClass == null || driverConstructor == null) {
                try {
                    try {
                        dialect = clazz.newInstance();
                    } catch (InstantiationException e2) {
                        final String error = StringTemplates.formatWithPlaceholder("Class {}  need a <init>()", Reflects.getFQNClassName(clazz));
                        throw new ClassFormatError(error);
                    } catch (IllegalAccessException e3) {
                        final String error = StringTemplates.formatWithPlaceholder("Class {}  need a <init>()", Reflects.getFQNClassName(clazz));
                        throw new ClassFormatError(error);
                    }
                } catch (Throwable ex) {
                    logger.error("Register dialect {} fail: {}", name, ex.getMessage(), ex);
                }
            } else {
                try {
                    try {
                        final Class<? extends java.sql.Driver> expectDriverClass = driverClass;
                        java.sql.Driver driver = Pipeline.<java.sql.Driver>of(DriverManager.getDrivers()).findFirst(new Predicate<java.sql.Driver>() {
                            @Override
                            public boolean test(java.sql.Driver d) {
                                return expectDriverClass.isInstance(d);
                            }
                        });
                        if (driver != null) {
                            driverConstructor.setAccessible(true);
                            dialect = driverConstructor.newInstance(driver);
                        }
                    } catch (InstantiationException e2) {
                        final String error = StringTemplates.formatWithPlaceholder("Class {}  need a <init>(Driver)", Reflects.getFQNClassName(clazz));
                        throw new ClassFormatError(error);
                    } catch (IllegalAccessException e3) {
                        final String error = StringTemplates.formatWithPlaceholder("Class {} need a public <init>(Driver", Reflects.getFQNClassName(clazz));
                        throw new ClassFormatError(error);
                    } catch (InvocationTargetException e) {
                        logger.error("Register dialect {} fail: {}", name, e.getMessage(), e);
                    }
                } catch (Throwable ex) {
                    logger.error("Register dialect {} fail: {}", name, ex.getMessage(), ex);
                }
            }
        }
        if (dialect != null) {
            DialectRegistry.nameToDialectMap.put(name, dialect);
            DialectRegistry.classNameToNameMap.put(clazz.getCanonicalName(), name);
            setDatabaseId(name, name);
        }
        return dialect;
    }


}
