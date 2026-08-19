#!/bin/bash
# Scans the codebase for EventBus.subscribe() calls that pass a lambda without a lifecycle parent.
# Passing an anonymous lambda directly to WeakHashMap-backed subscribers causes it to be immediately GC'd.
echo "Scanning for dangerous anonymous EventBus.subscribe(event -> ...) calls..."
grep -rnE "EventBus\\.subscribe\\([[:space:]]*[A-Za-z0-9_]+[[:space:]]*-\>" src/
if [ $? -ne 0 ]; then
  echo "No dangerous anonymous EventBus subscriptions found."
fi
