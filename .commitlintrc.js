import {RuleConfigSeverity} from "@commitlint/types";

/**
 * @type {import('@commitlint/types').UserConfig}
 */
const Configuration = {
    extends: ['@commitlint/config-conventional'],
    rules: {
        "scope-empty": [RuleConfigSeverity.Error, "never"],
        "scope-enum": [
            RuleConfigSeverity.Error,
            "always",
            [
                "lapis",
                "lapis-docs",
                "lapis-e2e",
                "github-actions",
                "root", // used by dependabot to update the root package.json
            ]
        ],
        "header-max-length": [RuleConfigSeverity.Disabled],
        "body-max-line-length": [RuleConfigSeverity.Disabled],
    }
};

export default Configuration;
