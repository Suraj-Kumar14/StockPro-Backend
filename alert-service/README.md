# Alert Service Notes

## SonarQube

Use `sonar.token` with the Maven Sonar goal. Do not use `sonar.login`, and do not leave a space before `-Dsonar.token`.

PowerShell example:

```powershell
mvn clean verify sonar:sonar "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=YOUR_VALID_TOKEN"
```

If you previously exposed or reused an old token, regenerate a new token in SonarQube and revoke the old one. Do not hardcode tokens in `pom.xml`.
