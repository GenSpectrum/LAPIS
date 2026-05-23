import { type FC } from 'react';
import { withBaseUrl } from './urls.ts';

type HelpTooltipProps = {
    /** Plain-text explanation shown on hover. */
    text: string;
    /**
     * Optional docs link target. If set, the icon also acts as a link that
     * opens the documentation page in a new tab. Should be an absolute path
     * starting with "/" (it is base-URL-prefixed at render time).
     */
    docsHref?: string;
};

/**
 * Small inline help affordance. Renders an ⓘ icon with a hover tooltip.
 * When `docsHref` is provided, clicking the icon opens the referenced
 * documentation page in a new tab.
 */
export const HelpTooltip: FC<HelpTooltipProps> = ({ text, docsHref }) => {
    const tip = docsHref ? `${text} (click to open the docs)` : text;
    const className =
        'tooltip tooltip-right inline-block align-middle text-base-content/50 hover:text-base-content text-xs font-mono ml-1.5 cursor-help leading-none';

    const symbol = <span aria-hidden='true'>(?)</span>;

    if (docsHref) {
        return (
            <a
                className={className}
                data-tip={tip}
                href={withBaseUrl(docsHref)}
                target='_blank'
                rel='noopener noreferrer'
                aria-label={text}
            >
                {symbol}
            </a>
        );
    }

    return (
        <span className={className} data-tip={tip} aria-label={text}>
            {symbol}
        </span>
    );
};
