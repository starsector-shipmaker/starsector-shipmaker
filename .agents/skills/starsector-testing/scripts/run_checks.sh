#!/bin/bash
# Helper script to execute all verification gates.
echo "=== Running Maven clean compile ==="
mvn clean compile
if [ $? -ne 0 ]; then
  echo "Compilation failed!"
  exit 1
fi

echo "=== Running Maven unit and integration tests ==="
mvn test
if [ $? -ne 0 ]; then
  echo "Tests failed!"
  exit 1
fi

echo "=== Running SpotBugs static analysis ==="
mvn spotbugs:check
if [ $? -ne 0 ]; then
  echo "SpotBugs static analysis flagged issues!"
  exit 1
fi

echo "=== Running codespell ==="
if command -v codespell &> /dev/null
then
  codespell --skip="*.class,*.jar,target,node_modules,.*,*.log" .
  if [ $? -ne 0 ]; then
    echo "codespell flagged typos!"
    exit 1
  fi
else
  echo "codespell not found, skipping typo check"
fi

echo "=== All verification gates passed successfully! ==="
exit 0
