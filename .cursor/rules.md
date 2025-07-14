# Cursor Rules for Modernizing ScoreKeeper

- All plans should be created in `.cursor/plans`.
- All plans should use checkboxes to track the completion state of action items or implementation steps.
- Do not add checkboxes to items that are informational, such as notes or descriptions.
- Use `bash` or `zsh` as the shell for development tasks.
- Any tool scripts should use `bash` as their shell.
- By default, the user will manually run build or test commands and provide the output. However, if the user requests, commands may be run directly—always show the command and ask for output unless instructed otherwise.
- When creating or updating plans, do not make changes to the project code or files (other than the plan files themselves). Only update plan files to reflect progress or changes. 