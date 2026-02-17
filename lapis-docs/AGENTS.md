# LAPIS Documentation - Development Guide for AI Coding Agents

This guide covers the LAPIS documentation website (TypeScript/Astro/Starlight).

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

### Code Patterns

```typescript
// Component props with explicit types
interface ButtonProps {
    label: string;
    onClick: () => void;
    variant?: 'primary' | 'secondary';
}

// Async operations
const fetchData = async (): Promise<ApiResponse> => {
    const response = await fetch('/api/data');
    return response.json();
};

// Destructuring
const { title, description } = props;
```

### Naming Conventions

- **Components:** PascalCase (e.g., `Navigation.astro`, `CodeBlock.tsx`)
- **Files:** kebab-case for pages (e.g., `getting-started.mdx`)
- **Variables/Functions:** camelCase (e.g., `fetchApiData`, `isActive`)
- **Constants:** UPPER_SNAKE_CASE for true constants (e.g., `API_BASE_URL`)

## Astro-Specific Guidelines

### Component Structure

```astro
---
// Component script (runs at build time)
import Layout from '../layouts/Layout.astro';

interface Props {
    title: string;
    description?: string;
}

const { title, description = 'Default description' } = Astro.props;
---

<Layout title={title}>
    <div class='content'>
        <h1>{title}</h1>
        {description && <p>{description}</p>}
    </div>
</Layout>

<style>
    .content {
        max-width: 800px;
        margin: 0 auto;
    }
</style>
```

### Markdown/MDX

- Use frontmatter for metadata
- Support for code syntax highlighting
- Can embed Astro/React components in MDX

```mdx
---
title: API Reference
description: Complete API documentation
---

import CodeBlock from '../../components/CodeBlock.astro';

# API Reference

<CodeBlock lang='bash'>curl https://api.example.com/endpoint</CodeBlock>
```

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

1. Locate the file in `src/content/` or `src/pages/`
2. Make changes
3. Preview with `npm run dev`
4. Verify formatting and types
5. Build with `npm run build` to catch any errors

### Adding Components

1. Create component in `src/components/`
2. Use `.astro` for Astro components, `.tsx` for React components
3. Export props interface if using TypeScript
4. Document component usage with JSDoc comments
5. Import and use in pages or other components

## Technology Stack

- **Astro:** Static site generator with islands architecture
- **TypeScript:** Full type safety
- **Markdown/MDX:** Content format
- **Node.js:** Runtime for build process
- **npm:** Package manager

## Important Development Notes

### Astro Islands

Astro uses "islands architecture" - interactive components are hydrated on demand:

```astro
<!-- Static by default (no JS shipped) -->
<StaticComponent />

<!-- Hydrate on page load -->
<InteractiveComponent client:load />

<!-- Hydrate when visible -->
<InteractiveComponent client:visible />

<!-- Hydrate on idle -->
<InteractiveComponent client:idle />
```

### Build Output

- Static HTML files generated at build time
- Minimal JavaScript shipped to client
- Fast page loads and excellent SEO

### Content Updates

Content changes require rebuilding:

```bash
npm run build
```

For continuous development:

```bash
npm run dev  # Auto-rebuilds on file changes
```

### CI/CD Integration

- Prettier formatting checked in CI
- TypeScript type checking enforced
- Build must succeed before merge
- Deploy on merge to main branch
