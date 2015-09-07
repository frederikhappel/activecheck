activecheck
===========

Activecheck is a standalone application written in Java that is intended to actively run checks on a host and submit the results and/or performance data (Nagios calls them passive checks) to an arbitrary number of collectors. This design moves the work of running checks from the collector (e.g. a Nagios Core installation) to the monitored host. Therefore, a single collector is capable of handling significantly more hosts and services as well as checks are better parallelized (faster, better freshness) as they are distributed to multiple hosts. Activecheck is capable of running checks in very short intervals so that even very small service disruptions can be detected.

It is extendable via plugins, so that check results (retrieved by an Activecheck reporter plugin) as well as the collector (managed through an Activecheck collector plugin) can be of any type. There are already reporter plugins for
- NRPE (can be used as drop-in replacement for all Nagios checks)
- JMX
- Graylog
- MongoDB

and collector plugins for
- NSCA (for compatibility but not recommended)
- NagMQ (recommended ZeroMQ based very fast plugin for Nagios/Naemon/Icinga, see https://github.com/jbreams/nagmq)
- Graphite (for directly graphing performance data)
