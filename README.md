# plug-field

A Clojurescript library providing record based presentation of keys/key-values in a Reagent/re-frame based app.

# DISCLAIMER

This library:

* Is experimental and intended to capture my personal usage and needs.
* Aims to reduce boilerplate and promote code re-use.
* Will have version "0.1.0-SNAPSHOT" while iterating on the design.

# Motivation

Much of what I do involves fetching data from a backend and then massage, augment and analyse it before presenting with
helpful visual clues, tooltips and interaction. This library is intended to streamline parts of that flow by making it
more declarative while still providing some customization options.

# Usage

## DEV

```shell
npm install
shadow-cljs watch app
```

[http://localhost:8000/](http://localhost:8000/)

## PROD

```clojure
;;soon
```

## License

Copyright © 2021 Morten Kristoffersen

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such
availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU
Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.
