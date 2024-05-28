import type { ReactNode } from 'react';

type Props = {
    children: ReactNode;
};

export const CodeBlock = ({ children }: Props) => (
    <pre className='overflow-x-auto bg-[var(--astro-code-color-background)] px-4 py-3 border border-solid border-[var(--sl-color-gray-5)]'>
        <code>{children}</code>
    </pre>
);
