import type { ReactNode } from 'react';

type Props = {
    children: ReactNode;
};

export const CodeBlock = ({ children }: Props) => (
    <pre className='overflow-x-auto bg-(--astro-code-color-background) px-4 py-3 border border-solid border-(--sl-color-gray-5)'>
        <code>{children}</code>
    </pre>
);
