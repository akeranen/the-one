# Contributing

If you would like to contribute to the development of the ONE simulator, please fork us on GitHub and send your contributions as pull requests.

If you are not sure where to start, new tests are always very welcome! Bugfixes are also highly appreciated.

If you want to add new features, there are some things to take into consideration:

* The preferred way to extend the ONE is by making new movement, routing, network interface, reporting, or application modules. See the existing modules for examples.
* If the new feature requires changes to existing classes, make sure the feature is generic and shared by many use cases.
* In particular one should avoid features that slow down simulation even when the new feature is not in use (e.g., if it requires checks for each simulation round)

When doing contributions, please try to follow the coding style of the existing code.
