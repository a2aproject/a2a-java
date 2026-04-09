# Contributing A2A SDK Integrations

To add your A2A SDK Integration for your chosen runtime to the list of integrations in the [README](README.md#server-integrations), open a pull request adding it to the list.

The pull request should contain a link to your project page.

Then the project page itself needs to contain the following information as a minimum:

* How to use the integration.
   * Ideally there should be a sample demonstrating how to use it 
* The integration should have tests, extending [AbstractA2AServerTest](tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerTest.java)
* The integration should pass the [TCK](https://github.com/a2aproject/a2a-tck), and make it obvious how to see that it has passed.
* Ideally, the integration should be deployed in Maven Central. If that is not possible, provide clear instructions for how to build it.

If some of the above points are problematic, feel free to point that out in your pull request, and we can discuss further. For example, AbstractA2AServerTest is currently written with only the initial runtimes in mind, and might need some tweaking.