package org.activecheck.plugin.reporter.mongodb;

import com.mongodb.Block;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.selector.ReadPreferenceServerSelector;
import com.mongodb.selector.ServerSelector;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ActivecheckPluginProperties(propertiesToMerge = {})
public class MongodbReporter extends ActivecheckReporter {
    private static final Logger logger = LoggerFactory
            .getLogger(MongodbReporter.class);
    private static final int MONGO_WAIT_FOR_RESULT = 10;
    private static final int MONGO_POOL_SIZE = 4;
    private static final ReadPreference MONGO_READ_PREFERENCE = ReadPreference
            .secondaryPreferred();
    private static final String[] EXCLUDE_DATABASES = {"local", "admin",
            "config"};

    private MongoClient mongoClient = null;
    private MongodbQuery mongodbQuery = null;
    private final List<String> databases = new ArrayList<String>();
    private final List<String> databasesToExclude = new ArrayList<String>();

    public MongodbReporter(PropertiesConfiguration properties) {
        super(properties);

        // initialize what has not been initialized
        reporterInit();
    }

    @Override
    protected void reporterInit() {
        // parse configuration file
        List<Object> stats = properties.getList("stats", null);
        if (stats != null) {
            mongodbQuery = new MongodbQueryStats(stats,
                    properties.getString("warning"),
                    properties.getString("critical"));
        } else {
            String queryWhere = StringUtils.join(
                    properties.getList("query_where"), ",");
            String queryFields = StringUtils.join(
                    properties.getList("query_fields"), ",");
            mongodbQuery = new MongodbQueryFind(queryWhere, queryFields,
                    properties.getString("warning"),
                    properties.getString("critical"));
        }

        // refresh mongo connection
        cleanUp();

        // databases to query
        databases.clear();
        for (Object database : properties.getList("databases")) {
            if (database instanceof String) {
                databases.add((String) database);
            }
        }

        // databases to exclude
        databasesToExclude.clear();
        for (Object database : properties.getList("databases_exclude")) {
            if (database instanceof String) {
                databasesToExclude.add((String) database);
            }
        }
        if (databasesToExclude.size() < 1) {
            databasesToExclude.addAll(Arrays.asList(EXCLUDE_DATABASES));
        }
    }

    private void connect() throws ActivecheckReporterException {
        if (mongoClient == null) {
            logger.debug("Cannot run query. MongoDB is not connected. Trying to (re)connect.");
            try {
                // configure credentials
                List<MongoCredential> credentialsList = new ArrayList<MongoCredential>();
                String username = properties
                        .getString("mongodb.username", null);
                String password = properties
                        .getString("mongodb.password", null);
                if (username != null && password != null) {
                    credentialsList.add(MongoCredential.createPlainCredential(
                            username, "*", password.toCharArray()));
                }

                // configure server addresses
                List<ServerAddress> addressList = new ArrayList<ServerAddress>();
                String socketPath = properties.getString("socket", null);
                if (socketPath != null) {
                    addressList.add(new ServerAddress(new AFUNIXSocketAddress(
                            new File(socketPath))));
                } else {
                    String url = properties.getString("url",
                            ServerAddress.defaultHost());
                    int port = ServerAddress.defaultPort();
                    String[] urlParts = url.split(":");
                    if (urlParts.length > 1) {
                        port = Integer.parseInt(urlParts[1]);
                    }
                    addressList.add(new ServerAddress(urlParts[0], port));
                }
                ServerSelector serverSelector = new ReadPreferenceServerSelector(
                        MONGO_READ_PREFERENCE);
                ClusterSettings clusterSettings = ClusterSettings.builder()
                        .hosts(addressList).serverSelector(serverSelector)
                        .build();

                // actually configure and (re)create mongoClient
                ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings
                        .builder().maxSize(MONGO_POOL_SIZE).build();
                MongoClientSettings settings = MongoClientSettings.builder()
                        .readPreference(MONGO_READ_PREFERENCE)
                        .credentialList(credentialsList)
                        .clusterSettings(clusterSettings)
                        .connectionPoolSettings(connectionPoolSettings).build();
                mongoClient = MongoClients.create(settings);
            } catch (Exception e) {
                mongoClient = null;
                String errorMessage = "MongodbReporter Configuration Error for service '"
                        + getOverallServiceName() + "': " + e.getMessage();

                logger.error(errorMessage);
                logger.trace(e.getMessage(), e);

                // set report and status
                setOverallServiceReport(NagiosServiceStatus.CRITICAL,
                        errorMessage);
                throw new ActivecheckReporterException(e);
            }
        }
    }

    @Override
    public void runCommand() throws ActivecheckReporterException {
        if (mongodbQuery == null) {
            String errorMessage = "no query has been defined for service '"
                    + getOverallServiceName() + "'";
            logger.error(errorMessage);
            setOverallServiceReport(NagiosServiceStatus.WARNING, errorMessage);
        } else {
            // (re)connect mongodb and execute query
            connect();

            try {
                final List<String> queryDatabases = databases;
                if (queryDatabases.size() < 1) {
                    final CountDownLatch listDbsLatch = new CountDownLatch(1);
                    mongoClient.listDatabaseNames().forEach(
                            new Block<String>() {
                                @Override
                                public void apply(final String database) {
                                    if (!databasesToExclude.contains(database)) {
                                        queryDatabases.add(database);
                                    }
                                }
                            }, new SingleResultCallback<Void>() {
                                @Override
                                public void onResult(Void arg0, Throwable t) {
                                    listDbsLatch.countDown();
                                }
                            });
                    listDbsLatch.await(MONGO_WAIT_FOR_RESULT, TimeUnit.SECONDS);
                }
                if (queryDatabases.size() > 0) {
                    mongodbQuery.getCheckResult().clear(NagiosServiceStatus.OK);

                    // run query on all databases
                    logger.debug("Querying databases: "
                            + StringUtils.join(queryDatabases, ","));
                    CountDownLatch latch = new CountDownLatch(
                            queryDatabases.size());
                    for (String database : queryDatabases) {
                        logger.debug("executing query '"
                                + mongodbQuery.getQuery()
                                + " on "
                                + mongoClient.getSettings()
                                .getClusterSettings().getHosts().get(0)
                                + "/" + database);

                        String[] parts = database.split("\\.", 2);
                        MongoDatabase db = mongoClient.getDatabase(parts[0]);
                        mongodbQuery.execute(db, (parts.length > 1) ? parts[1]
                                : null, latch);
                    }

                    // generate report
                    if (!latch.await(MONGO_WAIT_FOR_RESULT, TimeUnit.SECONDS)) {
                        mongodbQuery.getCheckResult().addMessage(
                                latch.getCount() + " of "
                                        + queryDatabases.size()
                                        + " queries did not return a result");
                    }
                    setOverallServiceReport(mongodbQuery.getCheckResult());
                } else {
                    setOverallServiceReport(NagiosServiceStatus.WARNING,
                            "No databases given to query");
                }
            } catch (Exception e) {
                String errorMessage = "MongodbReporter Error for service '"
                        + getOverallServiceName() + "': " + e.getMessage();

                logger.error(errorMessage);
                logger.trace(e.getMessage(), e);

                // set report and status
                setOverallServiceReport(NagiosServiceStatus.CRITICAL,
                        errorMessage);
                throw new ActivecheckReporterException(e);
            }
        }
    }

    @Override
    protected void cleanUp() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception e) {
                logger.error("Failed disconnecting MongoDB Connection: "
                        + e.getMessage());
                logger.trace(e.getMessage(), e);
            }
            mongoClient = null;
        }
    }
}
