import { useState } from 'react';
import { BasicInformationWizard } from './BasicInformationWizard.tsx';
import { MetadataWizard } from './MetadataWizard.tsx';
import { AdditionalInformationWizard } from './AdditionalInformationWizard.tsx';

const steps = ['Basic', 'Metadata', 'Additional'] as const;

export function ConfigWizard() {
    const [activeStep, setActiveStep] = useState(0);

    const handleNext = () => {
        if (activeStep < steps.length - 1) {
            setActiveStep(activeStep + 1);
        }
    };

    const handleBack = () => {
        if (activeStep > 0) {
            setActiveStep(activeStep - 1);
        }
    };

    return (
        <div className='flex flex-col border border-base-300 rounded-sm bg-base-100'>
            <div className='flex border-b border-base-300 text-sm font-mono'>
                {steps.map((label, index) => {
                    const isActive = index === activeStep;
                    const isPast = index < activeStep;
                    return (
                        <button
                            key={label}
                            onClick={() => setActiveStep(index)}
                            className={`flex-1 px-4 py-2 text-left border-r last:border-r-0 border-base-300 ${
                                isActive
                                    ? 'bg-base-200 font-semibold'
                                    : isPast
                                      ? 'text-base-content/80 hover:bg-base-200'
                                      : 'text-base-content/50 hover:bg-base-200'
                            }`}
                            type='button'
                        >
                            <span className='text-base-content/50'>{index + 1}.</span> {label}
                        </button>
                    );
                })}
            </div>
            <div className='p-5 space-y-4'>
                <WizardForStep step={steps[activeStep]} />
            </div>
            <div className='sticky bottom-0 flex items-center justify-between gap-2 px-5 py-3 border-t border-base-300 bg-base-100'>
                <button
                    type='button'
                    className='btn btn-sm btn-outline'
                    onClick={handleBack}
                    disabled={activeStep <= 0}
                >
                    ← Back
                </button>
                <span className='text-xs text-base-content/60 font-mono'>
                    Step {activeStep + 1} of {steps.length}
                </span>
                <button
                    type='button'
                    className='btn btn-sm btn-outline'
                    onClick={handleNext}
                    disabled={activeStep >= steps.length - 1}
                >
                    Next →
                </button>
            </div>
        </div>
    );
}

function WizardForStep({ step }: { step: (typeof steps)[number] }) {
    switch (step) {
        case 'Basic':
            return <BasicInformationWizard />;
        case 'Metadata':
            return <MetadataWizard />;
        case 'Additional':
            return <AdditionalInformationWizard />;
    }
}
