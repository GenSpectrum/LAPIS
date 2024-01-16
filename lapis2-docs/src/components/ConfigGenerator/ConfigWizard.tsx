import { useState } from 'react';
import { BasicInformationWizard } from './BasicInformationWizard.tsx';
import { MetadataWizard } from './MetadataWizard.tsx';
import { AdditionalInformationWizard } from './AdditionalInformationWizard.tsx';

const steps = ['Basic Information', 'Metadata', 'Additional Information'] as const;

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

    const handleStep = (step: number) => () => {
        setActiveStep(step);
    };

    return (
        <div className='w-full items-stretch '>
            <div className='steps'>
                {steps.map((label, index) => (
                    <a
                        key={label}
                        onClick={handleStep(index)}
                        className={`step ${index <= activeStep ? 'step-primary' : ''}`}
                    >
                        {label}
                    </a>
                ))}
            </div>
            <div className='flex flex-col items-center'>
                <WizardForStep step={steps[activeStep]} />
                <div className='w-full flex items-center justify-between flex-wrap'>
                    <button className='btn btn-outline' onClick={handleBack} disabled={activeStep <= 0}>
                        Back
                    </button>
                    <button className='btn btn-outline' onClick={handleNext} disabled={activeStep >= steps.length - 1}>
                        Next
                    </button>
                </div>
            </div>
        </div>
    );
}

function WizardForStep({ step }: { step: (typeof steps)[number] }) {
    switch (step) {
        case 'Basic Information':
            return <BasicInformationWizard />;
        case 'Metadata':
            return <MetadataWizard />;
        case 'Additional Information':
            return <AdditionalInformationWizard />;
    }
}
