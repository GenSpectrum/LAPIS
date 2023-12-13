import React, { type FC, type ReactElement, type ReactNode, useState } from 'react';

type Props = {
    children: React.ReactNode;
};

export const TabsBox = ({ children }: Props) => {
    const childrenAsArray = React.Children.toArray(children);
    const tabs: ReactElement<TabProps, typeof Tab>[] = [];
    for (const child of childrenAsArray) {
        if (nodeIsTab(child)) {
            tabs.push(child);
        } else {
            throw new Error('Found an invalid child type in TabsBoxReact.');
        }
    }

    const [activeTab, setActiveTab] = useState(0);

    return (
        <div>
            <div className='tabs'>
                {tabs.map((tab, index) => (
                    <a
                        key={tab.props.label}
                        className={`tab tab-lifted ${index === activeTab ? 'tab-active' : ''}`}
                        onClick={() => setActiveTab(index)}
                    >
                        {tab.props.label}
                    </a>
                ))}
            </div>
            <div className='-mt-px border border-solid border-gray-200 p-8'>{tabs[activeTab].props.children}</div>
        </div>
    );
};

type TabProps = {
    children: ReactNode;
    label: string;
};

export const Tab: FC<TabProps> = () => {
    return null;
};

function nodeIsTab(node: ReactNode | {}): node is ReactElement<TabProps, typeof Tab> {
    // return typeof node === 'object' && (node as any).type === Tab;
    return true;
}
