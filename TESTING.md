# UMS – GitHub Actions demo: 4 passing tests, 2 deliberately failing tests

This is a **demonstration** of how CI detects test failures. Do not merge the deliberately
failing assertions as your project's permanent quality gate. No production Java code was
changed. Java 21, Spring Boot, Maven, JUnit 5, Mockito, MockMvc, and test-only H2 are used.

## Expected results

| ID | Type | What it tests | Intentional CI result |
| --- | --- | --- | --- |
| UT-01 | Unit | Student creation encrypts password and sets role | PASS |
| UT-02 | Unit | Duplicate username conflict | FAIL ON PURPOSE: API returns 409 but assertion expects 400 |
| UT-03 | Unit | User updates name and password | PASS |
| IT-01 | Integration | Admin authenticated GET /api/me | PASS |
| IT-02 | Integration | Admin creates and retrieves persisted student | PASS |
| IT-03 | Integration | Student denied teacher endpoint | FAIL ON PURPOSE: API returns 403 but assertion expects 200 |

## Upload the files to GitHub

If your project is already on GitHub, use `ums_four_pass_two_fail_patch.zip`: extract it
and copy its contents into the repository root, preserving directories. Commit the
updates to `pom.xml`, `.github/workflows/tests.yml`, `src/test/...`, `scripts/...`,
and `TESTING.md`.

For a new repository, use `ums_four_pass_two_fail_full_project.zip`: extract the `ums`
folder and upload **the contents** of that folder to the repo root. DO NOT upload the ZIP
as a ZIP; GitHub will store it as a single file instead of unpacking it. Make sure
`pom.xml` is directly at the root and `.github/workflows/tests.yml` is present.

1. On GitHub, select `Add file` > `Upload files`, drag the extracted files/folders,
   and choose `Commit changes`. If you cannot upload hidden `.github` directories,
   create `.github/workflows/tests.yml` using `Add file` > `Create new file` and paste
   its YAML content.
2. Select your repo's `Actions` tab and choose `UMS - 4 Pass, 2 Intentional Failures`.
3. Select `Run workflow` > `Run workflow` (or use the run triggered by your commit).
4. Open the latest run. It should show a **red X**, by design. Open the `tests` job.
5. Expand `Print and publish all 6 individual results` to see a Markdown table.
   The same table appears on the workflow run **Summary** page.
6. Download the `ums-test-reports` artifact to get XML/TXT Surefire + Failsafe reports.

## Why Maven is called with `-Dmaven.test.failure.ignore=true`

A normal `mvn verify` stops after a failing unit test and may never run the integration
tests. For this exercise the workflow temporarily ignores test failures **during test
execution only**, allowing all six test cases to run. The final report step explicitly
exits with status 1, so GitHub Actions correctly marks the job **failed**.

This override is scoped to the workflow command; the pom does not globally ignore failures.

## Change the demo into a genuine all-green CI later

1. In `UserControllerTest`, change `HttpStatus.BAD_REQUEST` back to `HttpStatus.CONFLICT`
   in the UT-02 assertion.
2. In `UserApiIT`, change `status().isOk()` back to `status().isForbidden()` in IT-03.
3. Change the workflow's test command to `mvn --batch-mode --no-transfer-progress clean verify`.
4. Replace `python3 scripts/report_tests.py` with an ordinary reporting step (or
   remove the report check, because it intentionally exits 1).

**Note**: CI result is not yet verified by an actual Maven execution in this environment.
The original project seeds a fixed admin password. Do not publicly deploy with that default.
