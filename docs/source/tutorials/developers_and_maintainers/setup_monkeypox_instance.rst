Setup LAPIS for monkeypox
=========================

In this tutorial, you will setup a LAPIS instance for monkeypox. You will learn:

- how import data into LAPIS' database
- how to start the API server

Note: This setup is only for testing/demonstration purposes and not recommended for productive use.


Requirements
------------

- A working installation of `Docker <https://www.docker.com/>`_ and basic familiarity with using Docker


Data download and alignment
---------------------------

To get started, LAPIS needs some metadata, the unaligned sequences and, if available, aligned sequences. For monkeypox, example data is available in `Nextstrain's monkeypox repository <https://github.com/nextstrain/monkeypox/tree/master/example_data>`_. Please download the ``metadata.tsv`` and ``sequences.fasta``. They are already correctly formatted.

Next, the sequences should be aligned. It is possible to start LAPIS without an alignment (in that case, create an empty ``aligned.fasta``) but in order to filter and aggregate by mutations, an alignment is needed. You can use `Nextclade version 2 <https://nextclade.vercel.app/>`_ to obtain an alignment. Open the website, select "Monkeypox" as the pathogen, select the sequences.fasta file that you downloaded and click an the "Run" button. It can take a few moments until the sequences are analyzed. Once it has finished, you can click on the download symbol on the top right and download the ``nextclade.aligned.fasta`` file. Rename the file to ``aligned.fasta``.

Place the files metadata.tsv, sequences.fata and aligned.fasta into the same directory.

.. code-block:: bash

    /path/to/data
        |-- metadata.tsv
        |-- sequences.fasta
        |-- aligned.fasta


Initialize database
-------------------

LAPIS uses a PostgreSQL database (version 14+). An easy option to set up a database is to use the `postges Docker image <https://hub.docker.com/_/postgres/>`_. First, download the SQL scripts in `from here <https://github.com/cevo-public/LAPIS/tree/mpox/database>`_ and place them in the same directory. Open ``01_users.sql`` and set the passwords for the users.

.. code-block:: bash

    /path/to/db-scripts
        |-- 01_users.sql
        |-- 02_init.sql
        |-- 03_transform_and_merge.sql

Execute the following command to start the database. Change the password for the ``postgres`` user in the command.

.. code-block:: bash

    docker network create -d bridge lapis_network

    docker run -d \
        --name lapis_db \
        --net lapis_network \
        -e POSTGRES_PASSWORD=<missing> \
        -v /path/to/db-scripts:/docker-entrypoint-initdb.d \
        postgres:14



Data import
-----------

First, create a file ``lapis-proc-config.yml`` with the following content (please fill in the password):

.. code-block:: bash

    vineyard:
      host: lapis_db
      port: 5432
      dbname: postgres
      username: lapis_proc
      password: <missing>
      schema: public
    workdir: /data
    maxNumberWorkers: 20


Then, execute the following command:

.. code-block:: bash

    docker run --rm \
        --name lapis_proc \
        --net lapis_network \
        --entrypoint java \
        -v /path/to/lapis-proc-config.yml:/app/lapis-config.yml \
        -v /path/to/data:/data \
        ghcr.io/cevo-public/lapis-server:br-mpox \
            -jar /app/lapis.jar \
            --config /app/lapis-config.yml \
            Lapis --update-data load-mpox,transform-mpox,switch-in-staging 



API server
----------

To start the API server, create a file ``lapis-api-config.yml`` with the following content (please fill in the password):

.. code-block:: bash

    vineyard:
      host: lapis_db
      port: 5432
      dbname: postgres
      username: lapis_api
      password: <missing>
      schema: public
    cacheEnabled: false
    redisHost:
    redisPort:
    apiOpennessLevel: OPEN

Then, execute the following command:

.. code-block:: bash

    docker run --rm \
        --name lapis_api \
        --net lapis_network \
        -v /path/to/lapis-api-config.yml:/app/lapis-config.yml \
        -p 127.0.0.1:2345:2345 \
        ghcr.io/cevo-public/lapis-server:br-mpox


Wait half a minute and then open http://localhost:2345/v1/sample/aggregated in your browser.


.. toctree::
   :maxdepth: 3
