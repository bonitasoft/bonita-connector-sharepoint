# Generation Report: bonita-connector-sharepoint

## Summary

| Field | Value |
|---|---|
| Connector | bonita-connector-sharepoint |
| Generated | 2026-03-20 |
| Version | 1.0.0-beta.1 |
| Operations | 8 |
| Tests | 289 (281 passed, 8 skipped - integration tests) |
| Build | GREEN (`mvn clean verify`) |
| Java | 17 |
| SDK | Microsoft Graph SDK v6 with Azure Identity OAuth 2.0 |

## Operations

| # | Operation | Connector Class | Definition |
|---|---|---|---|
| 1 | upload-file | UploadFileConnector | sharepoint-upload-file.def |
| 2 | download-file | DownloadFileConnector | sharepoint-download-file.def |
| 3 | create-folder | CreateFolderConnector | sharepoint-create-folder.def |
| 4 | list-children | ListChildrenConnector | sharepoint-list-children.def |
| 5 | delete-item | DeleteItemConnector | sharepoint-delete-item.def |
| 6 | create-list-item | CreateListItemConnector | sharepoint-create-list-item.def |
| 7 | get-list-item | GetListItemConnector | sharepoint-get-list-item.def |
| 8 | update-list-item | UpdateListItemConnector | sharepoint-update-list-item.def |

## Test Coverage

| Metric | Covered | Total | Coverage |
|---|---|---|---|
| Instructions | 1,466 | 4,484 | 32% |
| Branches | 67 | 442 | 15% |
| Lines | 272 | 755 | 36% |
| Methods | 101 | 193 | 52% |
| Classes | 21 | 22 | 95% |

Note: Coverage is measured by unit tests only. Integration tests (8 skipped) require SharePoint API credentials and will increase coverage significantly when run with `-PITs`.

## Files Created

### Source Files (src/main/java)
- `AbstractSharePointConnector.java` - Base connector with shared auth and Graph client setup
- `SharePointClient.java` - Microsoft Graph API client wrapper
- `SharePointConfiguration.java` - Configuration builder (tenant, client ID, secret, site)
- `SharePointException.java` - Custom exception type
- `RetryPolicy.java` - Retry logic for transient Graph API failures
- `UploadFileConnector.java` / `UploadFileResult.java`
- `DownloadFileConnector.java` / `DownloadFileResult.java`
- `CreateFolderConnector.java` / `CreateFolderResult.java`
- `ListChildrenConnector.java` / `ListChildrenResult.java`
- `DeleteItemConnector.java` / `DeleteItemResult.java`
- `CreateListItemConnector.java` / `CreateListItemResult.java`
- `GetListItemConnector.java` / `GetListItemResult.java`
- `UpdateListItemConnector.java` / `UpdateListItemResult.java`

### Connector Definitions (src/main/resources-filtered)
- 8 `.def` files (connector definitions)
- 8 `.impl` files (connector implementations)

### Properties (src/main/resources)
- 8 `.properties` files (i18n labels)
- `sharepoint.png` (connector icon)

### Assembly Descriptors (src/assembly)
- `all-assembly.xml` (all operations in one ZIP)
- 8 per-operation assembly descriptors

### Test Files (src/test/java)
- `ConnectorTestToolkit.java` - Shared test utilities
- 8 `*ConnectorTest.java` - Unit tests (one per operation)
- 8 `*ConnectorPropertyTest.java` - Property-based tests (jqwik)
- 8 `*ConnectorIntegrationTest.java` - Integration tests (require credentials)
- `SharePointConnectorIT.java` - Cross-operation integration test

### CI/CD (.github/workflows)
- `build.yml` - Main branch build
- `build-pr.yml` - Pull request build
- `release.yml` - Release workflow with marketplace notification
- `claude-code-review.yml` - AI code review
- `claude.yml` - Claude Code workflow
- `beta-status.yml` - Beta progress tracker

### Beta Lifecycle
- `beta-status.json` - Machine-readable beta status
- `BETA_STATUS.md` - Human-readable beta status table
- `.github/ISSUE_TEMPLATE/beta-feedback.yml` - Beta feedback issue template

### Other
- `pom.xml` - Maven build configuration
- `CLAUDE.md` - AI assistant instructions
- `README.md` - Project documentation
- `LICENSE` - License file
- `.gitignore` - Git ignore rules
- `.github/dependabot.yml` - Dependabot configuration

## TODOs

- [ ] Configure SharePoint API credentials as GitHub repository secrets for integration tests
- [ ] Run integration tests against a real SharePoint site (`mvn verify -PITs`)
- [ ] Collect beta feedback from at least one customer deployment per operation
- [ ] Graduate to 1.0.0 release once all 8 operations are validated
- [ ] Publish to Bonita Marketplace after graduation
- [ ] Add connector documentation to bonita-doc (PR targeting `2024.3` branch)
