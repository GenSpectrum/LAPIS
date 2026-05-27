# LAPIS Documentation - Development Guide for AI Coding Agents

This guide covers the LAPIS documentation website (TypeScript/Astro/Starlight).

Docs page is assumed to be deployed alongside actual running LAPIS instance and serves primarily as documentation for that instance.
Field names mentioned on pages (e.g. in examples) should reference fields that actually exist in that instance
(rather than generic names like `stringField`).

Content organization is inspired by these categories:

- Tutorials
    - Learning-oriented: Aimed at someone who is new to the project, foolproof, starting-at-zero guide
    - Example: Teaching a child to cook
- Concepts
    - Problem-oriented: How to solve a common problem, reasonable starting point (not at zero)
    - Example: Food recipe
- References
    - Information-oriented: Describes classes, methods, APIs, etc.
    - Example: Wikipedia article on an ingredient

## Build, Test, and Lint Commands

```bash
cd lapis-docs

# Setup
npm install

# Development
npm run dev                       # Start dev server (usually http://localhost:4321)
npm run build                     # Build for production
npm run preview                   # Preview production build

# Code quality
npm run check-format              # Check Prettier formatting
npm run format                    # Auto-fix formatting
npm run check-types               # TypeScript type checking
```

## Project Structure

```
lapis-docs/
├── src/
│   ├── content/          # Markdown/MDX documentation content
│   ├── components/       # Reusable UI components
├── public/               # Static assets
├── astro.config.mjs      # Astro and Starlight configuration
└── package.json
```

## Code Style Guidelines

### TypeScript

- **Strict mode enabled** - Full type safety
- **Prefer explicit types** for function parameters and return values
- **Use interfaces** for object shapes
- **Avoid `any`** - use `unknown` if type is truly unknown
- **Be explicit** - use strict equality:
    - Avoid: `if (string) { ... }`. Instead: `if (string !== undefined) { ... }`
    - Avoid: `if (maybeValue != null) { ... }`. Instead: `if (maybeValue !== undefined && maybeValue !== null) { ... }`

### Naming Conventions

- **Components:** PascalCase (e.g., `Navigation.astro`, `CodeBlock.tsx`)
- **Files:** kebab-case for pages (e.g., `getting-started.mdx`)
- **Variables/Functions:** camelCase (e.g., `fetchApiData`, `isActive`)
- **Constants:** UPPER_SNAKE_CASE for true constants (e.g., `API_BASE_URL`)

## Documentation Best Practices

### Writing Style

- **Clear and concise** - Avoid unnecessary jargon
- **Active voice** - "Use this command" not "This command can be used"
- **Examples first** - Show working examples before detailed explanations
- **Code snippets** - Include runnable examples
- **Cross-references** - Link to related documentation

### Content Organization

1. **Overview** - What is this feature?
2. **Quick Start** - Minimal working example
3. **Detailed Guide** - In-depth explanation
4. **API Reference** - Technical specifications
5. **Examples** - Real-world use cases

## Development Workflow

### Adding New Documentation

1. Create new `.md` or `.mdx` file in `src/content/`
2. Add page to Astro config
3. Add frontmatter with title and description
4. Write content using markdown
5. Update navigation if needed
6. Preview with `npm run dev`
7. Run `npm run format` to fix formatting issues

### Updating Existing Documentation

1. Locate the file in `src/content/`
2. Make changes
3. Preview with `npm run dev`
4. Verify formatting and types
5. Build with `npm run build` to catch any errors

### Adding Components

1. Create component in `src/components/`
2. Use `.astro` for Astro components, `.tsx` for React components
3. Export props interface

## Technology Stack

- **Astro:** Static site generator with islands architecture
- **TypeScript:** Full type safety
- **Markdown/MDX:** Content format
- **Node.js:** Runtime for build process
- **npm:** Package manager

## Important Development Notes

Slight hack in "production":
The Dockerfile builds the code when starting since the build process needs access to the database config.
