#!/usr/bin/env python3
"""Read standard JUnit XML reports and show a 6-test GitHub Actions summary."""
from pathlib import Path
import os
import sys
import xml.etree.ElementTree as ET

EXPECTED_RESULTS = {
    "UT01_createStudentEncryptsPasswordAndAssignsRole": "PASS",
    "UT02_DEMO_FAIL_duplicateUsernameWrongExpectedStatus": "FAIL",
    "UT03_updateUserChangesNameAndEncryptsNewPassword": "PASS",
    "IT01_adminBasicLoginReturnsProfile": "PASS",
    "IT02_adminCanCreateAndRetrieveStudent": "PASS",
    "IT03_DEMO_FAIL_studentUnauthorizedAccessWrongExpectedStatus": "FAIL",
}
EXPECTED = len(EXPECTED_RESULTS)
EXPECTED_FAILURES = 2
report_paths = [
    ("Unit", Path("target/surefire-reports")),
    ("Integration", Path("target/failsafe-reports")),
]
rows = []
for category, directory in report_paths:
    for path in sorted(directory.glob("TEST-*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            print(f"ERROR: Malformed test report {path}: {exc}", file=sys.stderr)
            sys.exit(1)
        for case in root.iter("testcase"):
            label = case.attrib.get("name", "unknown")
            if case.find("failure") is not None:
                result = "FAIL"
            elif case.find("error") is not None:
                result = "ERROR"
            elif case.find("skipped") is not None:
                result = "SKIP"
            else:
                result = "PASS"
            rows.append((category, label, result))

passed = sum(result == "PASS" for _, _, result in rows)
failed = sum(result in ("FAIL", "ERROR") for _, _, result in rows)
skipped = sum(result == "SKIP" for _, _, result in rows)

summary = [
    "# UMS test execution results",
    "",
    "| Type | Test | Result |",
    "| --- | --- | --- |",
]
for category, label, result in rows:
    icon = {"PASS": "✅", "FAIL": "❌", "ERROR": "❌", "SKIP": "⚪"}[result]
    summary.append(f"| {category} | `{label}` | {icon} {result} |")
summary.extend(["", f"**Totals: {len(rows)} run; {passed} passed; {failed} failed; {skipped} skipped.**"])

print("\n".join(summary))
summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
if summary_path:
    with open(summary_path, "a", encoding="utf-8") as f:
        f.write("\n".join(summary) + "\n")

actual_results = {label: result for _, label, result in rows}
if (len(rows) != EXPECTED or actual_results != EXPECTED_RESULTS
        or passed != 4 or failed != EXPECTED_FAILURES or skipped):
    print("\nUnexpected totals: investigate the build or a genuine regression.", file=sys.stderr)
    sys.exit(1)

print("\nExpected 4 pass / 2 intentional failures confirmed. Marking demo job red.")
sys.exit(1)  # Deliberately FAIL the workflow because real test assertions failed.
