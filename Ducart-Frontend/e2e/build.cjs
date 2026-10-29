const { spawnSync } = require("child_process");

const result = spawnSync(process.execPath, [require.resolve("react-scripts/scripts/build")], {
  stdio: "inherit",
  env: {
    ...process.env,
    CI: "true",
    PUBLIC_URL: "",
    REACT_APP_GITHUB_PAGES: "false",
    REACT_APP_SERVER: "http://127.0.0.1:18080",
  },
});
if (result.error) throw result.error;
process.exit(result.status === null ? 1 : result.status);
